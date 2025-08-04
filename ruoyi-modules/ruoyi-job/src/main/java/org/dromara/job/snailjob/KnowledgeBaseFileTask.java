package org.dromara.job.snailjob;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.req.ResponseFormatRequest;
import org.dromara.system.constant.SysKnowledgeBaseDocumentConstants;
import org.dromara.system.domain.DocumentTask;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.domain.bo.SysKnowledgeBaseDocumentBo;
import org.dromara.system.domain.dto.DocumentParseRequest;
import org.dromara.system.domain.dto.DocumentParseResponse;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.IDocumentSplitService;
import org.dromara.system.service.IEmbeddingService;
import org.dromara.system.service.ISysKnowledgeBaseDocumentService;
import org.dromara.system.service.ISysModelConfigService;
import org.dromara.system.service.impl.DocumentTaskServiceImpl;
import org.dromara.system.service.impl.SysKnowledgeBaseDocumentServiceImpl;
import org.dromara.system.service.split.DocumentChunk;
import org.dromara.system.service.split.impl.TxtDocumentSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.dromara.system.service.IElasticsearchIndexService;

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

    private final SysKnowledgeBaseDocumentChunkMapper sysKnowledgeBaseDocumentChunkMapper;

    private final SysKnowledgeBaseDocumentServiceImpl sysKnowledgeBaseDocumentService;
    private final ISysModelConfigService iSysModelConfigService;

    private final TxtDocumentSplitter txtDocumentSplitter;

    private final IDocumentSplitService documentSplitService;
    private final AiService aiService;
    private final IEmbeddingService embeddingService;
    private final IElasticsearchIndexService elasticsearchIndexService;


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
                    .mode(doc.getMode() != null ? doc.getMode() : 1)
                    .fileUrl(doc.getUrl())
                    .enableImageRecognition(doc.getEnableImageRecognition() != null && doc.getEnableImageRecognition() == 1)
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
                            updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_CHUNK);

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

    @JobExecutor(name = "parseTxtFileJob")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult parseTxtFileJob(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行TXT文档分块任务");

            // Get document service
            // 1. 获取表sys_knowledge_base_document中状态为 document_chunk 的文档且文档类型不是xls和xlsx，分页获取
            Page<SysKnowledgeBaseDocument> page = new Page<>(1, BATCH_SIZE);
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getStatus, SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_CHUNK)
                .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
                .isNotNull(SysKnowledgeBaseDocument::getParseCompletedUrl)
                .ne(SysKnowledgeBaseDocument::getParseCompletedUrl, "")
                .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

            Page<SysKnowledgeBaseDocument> documentPage = sysKnowledgeBaseDocumentMapper.selectPage(page, queryWrapper);
            List<SysKnowledgeBaseDocument> documents = documentPage.getRecords();

            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待处理的TXT文档");
                return ExecuteResult.success("没有待处理的TXT文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理TXT文档", documents.size());

            int successCount = 0;
            int failCount = 0;

            // Default chunk settings
            final int DEFAULT_CHUNK_SIZE = 500;
            final int DEFAULT_OVERLAP_SIZE = 50;

            for (SysKnowledgeBaseDocument doc : documents) {
                try {
                    // 2. 拿到parseCompletedUrl字段，调用documentSplitService进行文档分块
                    String parseCompletedUrl = doc.getParseCompletedUrl();
                    Integer chunkSize = doc.getBlockSize() != null ? doc.getBlockSize() : DEFAULT_CHUNK_SIZE;
                    Integer overlapSize = doc.getOverlapSize() != null ? doc.getOverlapSize() : DEFAULT_OVERLAP_SIZE;

                    SnailJobLog.REMOTE.info("开始处理文档: {}, URL: {}", doc.getName(), parseCompletedUrl);

                    // 使用DocumentSplitService分割文档
                    List<DocumentChunk> chunks = documentSplitService.splitDocument(
                        parseCompletedUrl,
                        "txt",
                        chunkSize,
                        overlapSize
                    );

                    if (CollUtil.isNotEmpty(chunks)) {
                        // 3. 保存文档块到sys_knowledge_base_document_chunk表
                        for (DocumentChunk chunk : chunks) {
                            SysKnowledgeBaseDocumentChunk documentChunk = new SysKnowledgeBaseDocumentChunk();
                            documentChunk.setDocumentId(doc.getDocumentId());
                            documentChunk.setKnowledgeBaseId(doc.getKnowledgeBaseId());
                            documentChunk.setChunkIndex(chunk.getChunkIndex());
                            documentChunk.setContent(chunk.getContent());
                            documentChunk.setFileName(doc.getName());
                            documentChunk.setVectorStatus("0"); // 待处理
                            documentChunk.setMetadata(chunk.getMetadata());

                            sysKnowledgeBaseDocumentChunkMapper.insert(documentChunk);
                        }

                        // 4. 更新文档状态为已完成
                        SysKnowledgeBaseDocumentBo updateBo = new SysKnowledgeBaseDocumentBo();
                        updateBo.setDocumentId(doc.getDocumentId());
                        updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);

                        if (sysKnowledgeBaseDocumentService.updateByBo(updateBo)) {
                            successCount++;
                            SnailJobLog.REMOTE.info("文档 {} 分块成功，生成 {} 个块", doc.getName(), chunks.size());
                        } else {
                            failCount++;
                            SnailJobLog.REMOTE.error("文档 {} 状态更新失败", doc.getName());
                        }
                    } else {
                        SnailJobLog.REMOTE.warn("文档 {} 未生成任何块", doc.getName());
                        failCount++;
                    }

                } catch (Exception e) {
                    failCount++;
                    SnailJobLog.REMOTE.error("处理文档 {} 时发生错误: {}", doc.getName(), e.getMessage());

                    // 更新文档状态为失败
                    try {
                        SysKnowledgeBaseDocumentBo updateBo = new SysKnowledgeBaseDocumentBo();
                        updateBo.setDocumentId(doc.getDocumentId());
                        updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FAIL);
                        sysKnowledgeBaseDocumentService.updateByBo(updateBo);
                    } catch (Exception updateError) {
                        SnailJobLog.REMOTE.error("更新文档 {} 失败状态时出错: {}", doc.getName(), updateError.getMessage());
                    }
                }
            }

            SnailJobLog.REMOTE.info("TXT文档分块任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("TXT文档分块任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }

    }


    @JobExecutor(name = "documentChunkEmbeddingJob")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult documentChunkEmbeddingJob(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行文档向量化任务");

            // 1. 获取表sys_knowledge_base_document中状态为 document_embedding 的文档且文档类型不是xls和xlsx，分页获取
            Page<SysKnowledgeBaseDocument> page = new Page<>(1, BATCH_SIZE);
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getStatus, SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING)
                .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
                .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

            Page<SysKnowledgeBaseDocument> documentPage = sysKnowledgeBaseDocumentMapper.selectPage(page, queryWrapper);
            List<SysKnowledgeBaseDocument> documents = documentPage.getRecords();

            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待向量化的文档");
                return ExecuteResult.success("没有待向量化的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待向量化文档", documents.size());

            int successCount = 0;
            int failCount = 0;

            for (SysKnowledgeBaseDocument doc : documents) {
                try {
                    // 2. 根据文档id 去sys_knowledge_base_document_chunk表中查询vector_status==0 的数据
                    LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> chunkQueryWrapper = new LambdaQueryWrapper<>();
                    chunkQueryWrapper.eq(SysKnowledgeBaseDocumentChunk::getDocumentId, doc.getDocumentId())
                        .eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, "0")
                        .orderByAsc(SysKnowledgeBaseDocumentChunk::getChunkIndex);

                    List<SysKnowledgeBaseDocumentChunk> chunks = sysKnowledgeBaseDocumentChunkMapper.selectList(chunkQueryWrapper);

                    if (CollUtil.isEmpty(chunks)) {
                        SnailJobLog.REMOTE.info("文档 {} 没有待处理的分块", doc.getName());
                        continue;
                    }

                    SnailJobLog.REMOTE.info("文档 {} 找到 {} 个待向量化分块", doc.getName(), chunks.size());

                    int chunkSuccessCount = 0;

                    // 批量更新状态为处理中
                    List<Long> chunkIds = chunks.stream().map(SysKnowledgeBaseDocumentChunk::getChunkId).toList();
                    sysKnowledgeBaseDocumentChunkMapper.update(null,
                        new LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk>()
                            .in(SysKnowledgeBaseDocumentChunk::getChunkId, chunkIds)
                            .set(SysKnowledgeBaseDocumentChunk::getVectorStatus, "1"));

                    SysModelConfigVo sysModelConfigVo = iSysModelConfigService.queryById(doc.getModel());
                    IChatService chatService = AiService.getChatService(sysModelConfigVo.getModelProvider());

                    IChatRequest iChatRequest = new IChatRequest();
                    iChatRequest.setCode(sysModelConfigVo.getModelCode());
                    iChatRequest.setModel(sysModelConfigVo.getModelCode());
                    iChatRequest.setBaseUrl(sysModelConfigVo.getBaseUrl());
                    iChatRequest.setApiKey(sysModelConfigVo.getApiKey());
                    iChatRequest.setStream(Boolean.FALSE);
                    ResponseFormatRequest responseFormat = new ResponseFormatRequest();
                    responseFormat.setType(ResponseFormatRequest.Type.JSON_OBJECT);
                    iChatRequest.setResponseFormat(responseFormat);
                    // 处理每个分块
                    for (SysKnowledgeBaseDocumentChunk chunk : chunks) {
                        try {
                            iChatRequest.setPrompt(doc.getSlicePrompt().replace("{input}", chunk.getContent()));

                            // 收集完整的响应内容
                            StringBuilder responseContent = new StringBuilder();
                            chatService.stream(iChatRequest)
                                .doOnNext(response -> {
                                    if (response.getResult() != null &&
                                        response.getResult().getOutput() != null &&
                                        response.getResult().getOutput().getText() != null) {
                                        responseContent.append(response.getResult().getOutput().getText());
                                    }
                                })
                                .blockLast(); // 阻塞等待流完成

                            String completeResponse = responseContent.toString();

                            JSONObject responseJson = new JSONObject(completeResponse);

                            // Parse JSON to flat format
                            LinkedHashMap<String, String> parsedData = parseJsonToFlatFormat(responseJson);

                            // You can now use parsedData for further processing
                            // For example, log the parsed data or store it
                            SnailJobLog.REMOTE.debug("Parsed JSON data: {}", parsedData);

                            // 3. 生成向量
                            // 提取parsedData的所有key到列表中 key 向量化，value 作为content 为关键词查询
                            List<String> textList = new ArrayList<>(parsedData.keySet());

                            // 如果没有文本数据，跳过向量化
                            if (!textList.isEmpty()) {
                                // 批量向量化，每批20个
                                final int EMBEDDING_BATCH_SIZE = 20;
                                List<List<Double>> allEmbeddings = new ArrayList<>();

                                // 分批处理
                                for (int i = 0; i < textList.size(); i += EMBEDDING_BATCH_SIZE) {
                                    int endIndex = Math.min(i + EMBEDDING_BATCH_SIZE, textList.size());
                                    List<String> batch = textList.subList(i, endIndex);

                                    try {
                                        // 批量向量化
                                        List<List<Double>> batchEmbeddings = embeddingService.textsToEmbeddings(batch);
                                        allEmbeddings.addAll(batchEmbeddings);
                                        SnailJobLog.REMOTE.debug("成功向量化批次 {}/{}, 当前批次大小: {}",
                                            i / EMBEDDING_BATCH_SIZE + 1,
                                            (textList.size() + EMBEDDING_BATCH_SIZE - 1) / EMBEDDING_BATCH_SIZE,
                                            batch.size());
                                    } catch (Exception e) {
                                        SnailJobLog.REMOTE.error("批量向量化失败: {}", e.getMessage());
                                        throw new RuntimeException("向量化失败", e);
                                    }
                                }

                                // 批量准备ES文档
                                List<Map<String, Object>> esDocuments = new ArrayList<>();
                                List<String> keys = new ArrayList<>(parsedData.keySet());
                                String createTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                                // 验证chunk ID
                                if (chunk.getChunkId() == null) {
                                    throw new RuntimeException("文档块ID为空");
                                }

                                // 构建所有文档
                                for (int i = 0; i < keys.size() && i < allEmbeddings.size(); i++) {
                                    String key = keys.get(i);
                                    String value = parsedData.get(key);
                                    List<Double> embedding = allEmbeddings.get(i);

                                    // 构建ES文档数据
                                    Map<String, Object> esDocument = new HashMap<>();
                                    esDocument.put("documentId", chunk.getDocumentId().toString());
                                    esDocument.put("chunkId", chunk.getChunkId().toString() + "_" + IdUtil.fastSimpleUUID());
                                    esDocument.put("knowledgeBaseId", doc.getKnowledgeBaseId());
                                    esDocument.put("chunkTitle", key);  // 存储键作为标题
                                    esDocument.put("content", value);  // 存储值作为内容
                                    esDocument.put("fileName", chunk.getFileName());
                                    esDocument.put("metadata", chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
                                    esDocument.put("createTime", createTime);
                                    esDocument.put("embedding", embedding);

                                    esDocuments.add(esDocument);
                                }

                                // 批量存储到ES
                                if (!esDocuments.isEmpty()) {
                                    try {
                                        String indexName = elasticsearchIndexService.generateIndexName(
                                            doc.getKnowledgeBaseId().toString());

                                        int storedCount = elasticsearchIndexService.batchStoreDocuments(indexName, esDocuments);
                                        SnailJobLog.REMOTE.info("批量索引完成，成功存储 {}/{} 个文档到ES",
                                            storedCount, esDocuments.size());

                                        if (storedCount < esDocuments.size()) {
                                            throw new RuntimeException("部分文档存储失败");
                                        }
                                    } catch (Exception e) {
                                        SnailJobLog.REMOTE.error("批量索引到Elasticsearch失败: {}", e.getMessage());
                                        throw new RuntimeException("存储向量失败", e);
                                    }
                                }
                            }

                            // 4. 更新状态为已完成
                            chunk.setVectorStatus("2");
                            sysKnowledgeBaseDocumentChunkMapper.updateById(chunk);

                            chunkSuccessCount++;

                        } catch (Exception e) {
                            SnailJobLog.REMOTE.error("处理分块 {} 时发生错误: {}", chunk.getChunkId(), e.getMessage());
                            // 失败时将状态恢复为待处理
                            chunk.setVectorStatus("0");
                            sysKnowledgeBaseDocumentChunkMapper.updateById(chunk);
                        }
                    }

                    // 检查文档的所有分块是否都已完成
                    Long pendingCount = sysKnowledgeBaseDocumentChunkMapper.selectCount(
                        new LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk>()
                            .eq(SysKnowledgeBaseDocumentChunk::getDocumentId, doc.getDocumentId())
                            .ne(SysKnowledgeBaseDocumentChunk::getVectorStatus, "2")
                    );

                    if (pendingCount == 0) {
                        // 所有分块都已完成，更新文档状态
                        SysKnowledgeBaseDocumentBo updateBo = new SysKnowledgeBaseDocumentBo();
                        updateBo.setDocumentId(doc.getDocumentId());
                        updateBo.setStatus(SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FINISHED);

                        if (sysKnowledgeBaseDocumentService.updateByBo(updateBo)) {
                            successCount++;
                            SnailJobLog.REMOTE.info("文档 {} 向量化完成", doc.getName());
                        }
                    } else {
                        SnailJobLog.REMOTE.info("文档 {} 还有 {} 个分块未完成", doc.getName(), pendingCount);
                    }

                } catch (Exception e) {
                    failCount++;
                    SnailJobLog.REMOTE.error("处理文档 {} 时发生错误: {}", doc.getName(), e.getMessage());
                }
            }

            SnailJobLog.REMOTE.info("文档向量化任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("文档向量化任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }
    }

    /**
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

    /**
     * Parse JSON into flat format with specific transformation pattern
     * For each leaf value, creates two entries:
     * 1. path : path/nvalue
     * 2. path/nvalue : path/nvalue
     *
     * @param json the JSON object to parse
     * @return LinkedHashMap containing the transformed data
     */
    private LinkedHashMap<String, String> parseJsonToFlatFormat(JSONObject json) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        parseJsonRecursive(json, "", result);
        return result;
    }

    /**
     * Recursively parse JSON object and build path-value mappings
     *
     * @param obj         current JSON object or value
     * @param currentPath current path (comma-separated)
     * @param result      LinkedHashMap to store results
     */
    private void parseJsonRecursive(Object obj, String currentPath, LinkedHashMap<String, String> result) {
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newPath = currentPath.isEmpty() ? key : currentPath + "," + key;
                parseJsonRecursive(value, newPath, result);
            }
        } else {
            // Leaf node - create the two entries
            String leafValue = obj != null ? obj.toString() : "";
            String pathWithValue = currentPath + "/n" + leafValue;

            // First entry: path : path/nvalue
            result.put(currentPath, pathWithValue);
            // Second entry: path/nvalue : path/nvalue
            result.put(pathWithValue, pathWithValue);
        }
    }
}
