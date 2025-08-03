package org.dromara.job.snailjob;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.system.constant.SysKnowledgeBaseDocumentConstants;
import org.dromara.system.domain.DocumentTask;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.dto.DocumentParseRequest;
import org.dromara.system.domain.dto.DocumentParseResponse;
import org.dromara.system.domain.vo.SysKnowledgeBaseDocumentVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.ISysKnowledgeBaseDocumentService;
import org.dromara.system.service.impl.DocumentTaskServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseFileTask {
    @Autowired
    private DocumentTaskServiceImpl documentTaskService;

    @Qualifier("embeddingRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${document.parse.api.url:http://115.190.43.113:8898/parse_document}")
    private String documentParseApiUrl;


    private final SysKnowledgeBaseDocumentMapper sysKnowledgeBaseDocumentMapper;

    private static final int BATCH_SIZE = 10;

    @JobExecutor(name = "fileParsingJob")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult fileParsingJob(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行文档解析任务");

            // Get document service
            ISysKnowledgeBaseDocumentService documentService = SpringUtils.getBean(ISysKnowledgeBaseDocumentService.class);


            Page<SysKnowledgeBaseDocument> page = new Page<>(1, BATCH_SIZE);
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getStatus, SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED)
                .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xlx")
                .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

            Page<SysKnowledgeBaseDocument> documentPage = sysKnowledgeBaseDocumentMapper.selectPage(page, queryWrapper);

            // Filter out Excel files
            List<SysKnowledgeBaseDocument> documentsToProcess = documentPage.getRecords();

            if (CollUtil.isEmpty(documentsToProcess)) {
                SnailJobLog.REMOTE.info("没有待处理的文档");
                return ExecuteResult.success("没有待处理的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理文档", documentsToProcess.size());

            // Build request list
            List<DocumentParseRequest> requests = new ArrayList<>();
            for (SysKnowledgeBaseDocument doc : documentsToProcess) {
                DocumentParseRequest request = DocumentParseRequest.builder()
                    .fileType(doc.getType())
                    .mode(1)
                    .fileUrl(doc.getUrl())
                    .enableImageRecognition(StrUtil.isNotBlank(doc.getImagePrompt()))
                    .prompt(doc.getImagePrompt())
                    .build();
                requests.add(request);
            }

            // Call document parsing API
            DocumentParseResponse response = callDocumentParseApi(requests);

            int successCount = 0;
            int failCount = 0;

            if (response != null && response.getSuccessTaskIds() != null) {
                // Update document status and taskId
                for (Map.Entry<String, String> entry : response.getSuccessTaskIds().entrySet()) {
                    String fileUrl = entry.getKey();
                    String taskId = entry.getValue();

                    // Find document by URL
                    SysKnowledgeBaseDocument doc = documentsToProcess.stream()
                        .filter(d -> fileUrl.equals(d.getUrl()))
                        .findFirst()
                        .orElse(null);

                    if (doc != null) {
                        // Update document status and taskId
                        SysKnowledgeBaseDocumentBo updateBo = new SysKnowledgeBaseDocumentBo();
                        updateBo.setDocumentId(doc.getDocumentId());
                        updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING);
                        updateBo.setTaskId(taskId);

                        if (documentService.updateByBo(updateBo)) {
                            successCount++;
                            SnailJobLog.REMOTE.info("文档 {} 更新成功，任务ID: {}", doc.getName(), taskId);
                        } else {
                            failCount++;
                            SnailJobLog.REMOTE.error("文档 {} 更新失败", doc.getName());
                        }
                    }
                }

                failCount += documentsToProcess.size() - successCount;
            } else {
                failCount = documentsToProcess.size();
                SnailJobLog.REMOTE.error("文档解析API调用失败或返回空响应");
            }

            SnailJobLog.REMOTE.info("文档解析任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("文档解析任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }
    }



    @JobExecutor(name = "getCompletedDocumentUrl")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult getCompletedDocumentUrl(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行文档解析完成URL同步任务");

            // Get document service
            ISysKnowledgeBaseDocumentService documentService = SpringUtils.getBean(ISysKnowledgeBaseDocumentService.class);

            // 1. 获取sys_knowledge_base_document 表中 status 为to_be_executed 的文档，分页查询
            Page<SysKnowledgeBaseDocument> page = new Page<>(1, 100);
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getStatus, SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING)
                .isNotNull(SysKnowledgeBaseDocument::getTaskId)
                .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xlx")
                .ne(SysKnowledgeBaseDocument::getTaskId, "")
                .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

            Page<SysKnowledgeBaseDocument> documentPage = sysKnowledgeBaseDocumentMapper.selectPage(page, queryWrapper);
            List<SysKnowledgeBaseDocument> documents = documentPage.getRecords();

            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待处理的文档");
                return ExecuteResult.success("没有待处理的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理文档", documents.size());

            int successCount = 0;
            int failCount = 0;

            for (SysKnowledgeBaseDocument doc : documents) {
                try {
                    // 2. 根据taskId查询document_task表中status 为completed 的数据
                    List<DocumentTask> completedTasks = documentTaskService.queryByTaskIdAndStatus(doc.getTaskId(), "completed");

                    if (CollUtil.isNotEmpty(completedTasks)) {
                        DocumentTask completedTask = completedTasks.get(0); // 取第一个完成的任务

                        if (StrUtil.isNotBlank(completedTask.getProcessedUrl())) {
                            // 3. 将查询到的数据中的processed_url 存储到 parse_completed_url 字段中，并更新状态为 document_embedding
                            SysKnowledgeBaseDocumentBo updateBo = new SysKnowledgeBaseDocumentBo();
                            updateBo.setDocumentId(doc.getDocumentId());
                            updateBo.setParseCompletedUrl(completedTask.getProcessedUrl());
                            updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);

                            if (documentService.updateByBo(updateBo)) {
                                successCount++;
                                SnailJobLog.REMOTE.info("文档 {} 更新解析URL成功，状态已更新为 document_embedding", doc.getName());
                            } else {
                                failCount++;
                                SnailJobLog.REMOTE.error("文档 {} 更新解析URL失败", doc.getName());
                            }
                        } else {
                            SnailJobLog.REMOTE.info("文档 {} 的任务 {} 已完成但没有处理后URL", doc.getName(), doc.getTaskId());
                        }
                    } else {
                        SnailJobLog.REMOTE.info("文档 {} 的任务 {} 还未完成", doc.getName(), doc.getTaskId());
                    }
                } catch (Exception e) {
                    failCount++;
                    SnailJobLog.REMOTE.error("处理文档 {} 时发生错误: {}", doc.getName(), e.getMessage());
                }
            }

            SnailJobLog.REMOTE.info("文档解析URL同步任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("文档解析URL同步任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }
    }
    /**
     *
     * Call document parsing API
     *
     * @param requests document parse requests
     * @return parse response
     */
    private DocumentParseResponse callDocumentParseApi(List<DocumentParseRequest> requests) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<List<DocumentParseRequest>> entity = new HttpEntity<>(requests, headers);

            // Make the API call
            ResponseEntity<DocumentParseResponse> response = restTemplate.exchange(
                documentParseApiUrl,
                HttpMethod.POST,
                entity,
                DocumentParseResponse.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                SnailJobLog.REMOTE.error("文档解析API请求失败，状态码: {}", response.getStatusCode());
                return null;
            }

            return response.getBody();

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("调用文档解析API失败", exception);
            return null;
        }
    }
}
