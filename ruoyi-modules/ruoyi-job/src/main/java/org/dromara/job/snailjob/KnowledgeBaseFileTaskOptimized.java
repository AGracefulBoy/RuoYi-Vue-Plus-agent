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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.SpringUtils;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.req.ResponseFormatRequest;
import org.dromara.job.helper.BatchOperationHelper;
import org.dromara.system.constant.SysKnowledgeBaseDocumentConstants;
import org.dromara.system.domain.DocumentTask;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.domain.dto.DocumentParseRequest;
import org.dromara.system.domain.dto.DocumentParseResponse;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.*;
import org.dromara.system.service.impl.DocumentTaskServiceImpl;
import org.dromara.system.service.split.DocumentChunk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 优化后的知识库文件处理定时任务
 * 使用线程池并行处理，提高处理效率
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseFileTaskOptimized {

    @Qualifier("embeddingRestTemplate")
    private final RestTemplate restTemplate;

    @Qualifier("documentProcessingExecutor")
    private final ThreadPoolTaskExecutor documentProcessingExecutor;

    @Qualifier("embeddingExecutor")
    private final ThreadPoolTaskExecutor embeddingExecutor;

    private final SysKnowledgeBaseDocumentMapper documentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper chunkMapper;
    private final BatchOperationHelper batchOperationHelper;
    private final DocumentTaskServiceImpl documentTaskService;
    private final ISysModelConfigService modelConfigService;
    private final IDocumentSplitService documentSplitService;
    private final IEmbeddingService embeddingService;
    private final IElasticsearchIndexService elasticsearchIndexService;

    @Value("${document.parse.api.url:http://115.190.43.113:8898/parse_document}")
    private String documentParseApiUrl;

    @Value("${document.processing.batch-size:10}")
    private int batchSize;

    @Value("${document.processing.embedding.batch-size:20}")
    private int embeddingBatchSize;

    /**
     * 文档解析任务 - 优化版
     * 并行处理文档，批量调用API
     */
    @JobExecutor(name = "fileParsingJobOptimized")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult fileParsingJobOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版文档解析任务");

            // 批量获取待处理文档
            List<SysKnowledgeBaseDocument> documents = fetchDocumentsToProcess(batchSize);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待处理的文档");
                return ExecuteResult.success("没有待处理的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理文档", documents.size());

            // 构建批量请求
            List<DocumentParseRequest> requests = documents.stream()
                .map(this::buildParseRequest)
                .collect(Collectors.toList());

            // 批量调用解析API
            DocumentParseResponse response = callDocumentParseApi(requests);

            // 处理响应并批量更新
            int successCount = processParseResponseAndUpdate(documents, response);
            int failCount = documents.size() - successCount;

            SnailJobLog.REMOTE.info("文档解析任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("文档解析任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * 获取完成的文档URL - 优化版
     * 并行查询和更新
     */
    @JobExecutor(name = "getCompletedDocumentUrlOptimized")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult getCompletedDocumentUrlOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版文档解析URL同步任务");

            // 批量获取正在解析的文档
            List<SysKnowledgeBaseDocument> documents = fetchParsingDocuments(100);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待同步的文档");
                return ExecuteResult.success("没有待同步的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待同步文档", documents.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 并行处理文档URL同步
            List<CompletableFuture<Void>> futures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(() -> {
                    try {
                        if (syncDocumentUrl(doc)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        SnailJobLog.REMOTE.error("处理文档 {} 时发生错误", doc.getName(), e);
                    }
                }, documentProcessingExecutor))
                .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            SnailJobLog.REMOTE.info("文档URL同步任务完成: 成功 {} 个, 失败 {} 个",
                successCount.get(), failCount.get());
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个",
                successCount.get(), failCount.get()));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("文档URL同步任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * 文档分块任务 - 优化版
     * 并行处理文档分块
     */
    @JobExecutor(name = "parseTxtFileJobOptimized")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult parseTxtFileJobOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版文档分块任务");

            // 批量获取待分块文档
            List<SysKnowledgeBaseDocument> documents = fetchChunkingDocuments(batchSize);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待分块的文档");
                return ExecuteResult.success("没有待分块的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待分块文档", documents.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 并行处理文档分块
            List<CompletableFuture<Void>> futures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(() -> {
                    try {
                        if (processDocumentChunking(doc)) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        SnailJobLog.REMOTE.error("处理文档 {} 分块时发生错误", doc.getName(), e);
                        updateDocumentStatus(doc.getDocumentId(),
                            SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FAIL);
                    }
                }, documentProcessingExecutor))
                .toList();

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            SnailJobLog.REMOTE.info("文档分块任务完成: 成功 {} 个, 失败 {} 个",
                successCount.get(), failCount.get());
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个",
                successCount.get(), failCount.get()));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("文档分块任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * 文档向量化任务 - 优化版
     * 多级并行处理：文档级并行 + 批量向量化
     */
    @JobExecutor(name = "documentChunkEmbeddingJobOptimized")
    public ExecuteResult documentChunkEmbeddingJobOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版文档向量化任务");

            // 批量获取待向量化文档
            List<SysKnowledgeBaseDocument> documents = fetchEmbeddingDocuments(batchSize);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待向量化的文档");
                return ExecuteResult.success("没有待向量化的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待向量化文档", documents.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 文档级并行处理
            List<CompletableFuture<Void>> documentFutures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(() -> {
                    try {
                        if (processDocumentEmbedding(doc)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        SnailJobLog.REMOTE.error("处理文档 {} 向量化时发生错误", doc.getName(), e);
                    }
                }, embeddingExecutor))
                .collect(Collectors.toList());

            // 等待所有文档处理完成
            CompletableFuture.allOf(documentFutures.toArray(new CompletableFuture[0])).join();

            SnailJobLog.REMOTE.info("文档向量化任务完成: 成功 {} 个, 失败 {} 个",
                successCount.get(), failCount.get());
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个",
                successCount.get(), failCount.get()));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("文档向量化任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * 处理单个文档的向量化
     */
    private boolean processDocumentEmbedding(SysKnowledgeBaseDocument doc) {
        try {
            // 获取待处理的chunks
            List<SysKnowledgeBaseDocumentChunk> chunks = fetchPendingChunks(doc.getDocumentId());
            if (CollUtil.isEmpty(chunks)) {
                SnailJobLog.REMOTE.info("文档 {} 没有待处理的分块", doc.getName());
                return false;
            }

            SnailJobLog.REMOTE.info("文档 {} 找到 {} 个待向量化分块", doc.getName(), chunks.size());

            // 批量更新状态为处理中
            List<Long> chunkIds = chunks.stream()
                .map(SysKnowledgeBaseDocumentChunk::getChunkId)
                .collect(Collectors.toList());
            batchOperationHelper.batchUpdateChunkStatus(chunkIds, "1");

            // 获取模型配置
            SysModelConfigVo modelConfig = modelConfigService.queryById(doc.getModel());
            IChatService chatService = AiService.getChatService(modelConfig.getModelProvider());

            // 分批处理chunks
            List<List<SysKnowledgeBaseDocumentChunk>> chunkBatches =
                Lists.partition(chunks, embeddingBatchSize);

            List<CompletableFuture<List<Map<String, Object>>>> embeddingFutures =
                chunkBatches.stream()
                    .map(batch -> CompletableFuture.supplyAsync(() ->
                        processChunkBatch(batch, doc, chatService, modelConfig),
                        documentProcessingExecutor))
                    .toList();

            // 等待所有批次完成
            CompletableFuture.allOf(embeddingFutures.toArray(new CompletableFuture[0])).join();

            // 收集所有ES文档
            List<Map<String, Object>> allEsDocuments = embeddingFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

            // 批量存储到ES
            if (!allEsDocuments.isEmpty()) {
                String indexName = elasticsearchIndexService.generateIndexName(
                    doc.getKnowledgeBaseId().toString());
                int storedCount = elasticsearchIndexService.batchStoreDocuments(indexName, allEsDocuments);
                SnailJobLog.REMOTE.info("文档 {} 批量索引完成，成功存储 {}/{} 个文档到ES",
                    doc.getName(), storedCount, allEsDocuments.size());
            }

            // 批量更新chunk状态为完成
            batchOperationHelper.batchUpdateChunkStatus(chunkIds, "2");

            // 检查并更新文档状态
            if (batchOperationHelper.checkAllChunksCompleted(doc.getDocumentId())) {
                updateDocumentStatus(doc.getDocumentId(),
                    SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FINISHED);
                return true;
            }

            return false;

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("处理文档 {} 向量化失败", doc.getName(), e);
            throw new RuntimeException("文档向量化失败", e);
        }
    }

    /**
     * 处理chunk批次的向量化
     */
    private List<Map<String, Object>> processChunkBatch(
            List<SysKnowledgeBaseDocumentChunk> chunks,
            SysKnowledgeBaseDocument doc,
            IChatService chatService,
            SysModelConfigVo modelConfig) {

        List<Map<String, Object>> esDocuments = new ArrayList<>();

        for (SysKnowledgeBaseDocumentChunk chunk : chunks) {
            try {
                // 构建请求
                IChatRequest request = buildChatRequest(modelConfig, doc.getSlicePrompt(), chunk.getContent());

                // 收集响应
                StringBuilder responseContent = new StringBuilder();
                chatService.stream(request)
                    .doOnNext(response -> {
                        if (response.getResult() != null &&
                            response.getResult().getOutput() != null &&
                            response.getResult().getOutput().getText() != null) {
                            responseContent.append(response.getResult().getOutput().getText());
                        }
                    })
                    .blockLast();

                // 解析响应
                JSONObject responseJson = new JSONObject(responseContent.toString());
                LinkedHashMap<String, String> parsedData = parseJsonToFlatFormat(responseJson);

                // 生成向量
                List<String> textList = new ArrayList<>(parsedData.keySet());
                if (!textList.isEmpty()) {
                    List<List<Float>> embeddings = embeddingService.textsToEmbeddings(textList,doc.getEmbeddingModel());

                    // 构建ES文档
                    String createTime = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    for (int i = 0; i < textList.size() && i < embeddings.size(); i++) {
                        Map<String, Object> esDoc = buildEsDocument(
                            chunk, doc, textList.get(i), parsedData.get(textList.get(i)),
                            embeddings.get(i), createTime);
                        esDocuments.add(esDoc);
                    }
                }

            } catch (Exception e) {
                SnailJobLog.REMOTE.error("处理chunk {} 向量化失败", chunk.getChunkId(), e);
            }
        }

        return esDocuments;
    }

    // ========== 辅助方法 ==========

    private List<SysKnowledgeBaseDocument> fetchDocumentsToProcess(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED)
            .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    private List<SysKnowledgeBaseDocument> fetchParsingDocuments(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING)
            .isNotNull(SysKnowledgeBaseDocument::getTaskId)
            .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .ne(SysKnowledgeBaseDocument::getTaskId, "")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    private List<SysKnowledgeBaseDocument> fetchChunkingDocuments(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_CHUNK)
            .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .isNotNull(SysKnowledgeBaseDocument::getParseCompletedUrl)
            .ne(SysKnowledgeBaseDocument::getParseCompletedUrl, "")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    private List<SysKnowledgeBaseDocument> fetchEmbeddingDocuments(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING)
            .notIn(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    private List<SysKnowledgeBaseDocumentChunk> fetchPendingChunks(Long documentId) {
        LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> queryWrapper =
            new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId)
            .eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, "0")
            .orderByAsc(SysKnowledgeBaseDocumentChunk::getChunkIndex);

        return chunkMapper.selectList(queryWrapper);
    }

    private DocumentParseRequest buildParseRequest(SysKnowledgeBaseDocument doc) {
        return DocumentParseRequest.builder()
            .fileType(doc.getType())
            .mode(doc.getMode() != null ? doc.getMode() : 1)
            .fileUrl(doc.getUrl())
            .enableImageRecognition(doc.getEnableImageRecognition() != null &&
                doc.getEnableImageRecognition() == 1)
            .prompt(doc.getImagePrompt())
            .build();
    }

    private boolean syncDocumentUrl(SysKnowledgeBaseDocument doc) {
        List<DocumentTask> completedTasks = documentTaskService.queryByTaskIdAndStatus(
            doc.getTaskId(), "completed");

        if (CollUtil.isNotEmpty(completedTasks)) {
            DocumentTask completedTask = completedTasks.get(0);

            if (StrUtil.isNotBlank(completedTask.getProcessedUrl())) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("parseCompletedUrl", completedTask.getProcessedUrl());
                updates.put("status", SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_CHUNK);

                Map<Long, Map<String, Object>> documentUpdates = new HashMap<>();
                documentUpdates.put(doc.getDocumentId(), updates);

                int result = batchOperationHelper.batchUpdateDocuments(documentUpdates);
                if (result > 0) {
                    SnailJobLog.REMOTE.info("文档 {} 更新解析URL成功", doc.getName());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean processDocumentChunking(SysKnowledgeBaseDocument doc) {
        try {
            String parseCompletedUrl = doc.getParseCompletedUrl();
            Integer chunkSize = doc.getBlockSize() != null ? doc.getBlockSize() : 500;
            Integer overlapSize = doc.getOverlapSize() != null ? doc.getOverlapSize() : 50;

            SnailJobLog.REMOTE.info("开始处理文档分块: {}", doc.getName());

            // 分割文档
            List<DocumentChunk> chunks = documentSplitService.splitDocument(
                parseCompletedUrl, "txt", chunkSize, overlapSize);

            if (CollUtil.isNotEmpty(chunks)) {
                // 构建chunk实体列表
                List<SysKnowledgeBaseDocumentChunk> chunkEntities = chunks.stream()
                    .map(chunk -> buildChunkEntity(chunk, doc))
                    .collect(Collectors.toList());

                // 批量插入
                int insertedCount = batchOperationHelper.batchInsertChunks(chunkEntities);

                // 更新文档状态
                if (insertedCount > 0) {
                    updateDocumentStatus(doc.getDocumentId(),
                        SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);
                    SnailJobLog.REMOTE.info("文档 {} 分块成功，生成 {} 个块",
                        doc.getName(), insertedCount);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("文档 {} 分块失败", doc.getName(), e);
            throw new RuntimeException("文档分块失败", e);
        }
    }

    private SysKnowledgeBaseDocumentChunk buildChunkEntity(DocumentChunk chunk,
                                                           SysKnowledgeBaseDocument doc) {
        SysKnowledgeBaseDocumentChunk entity = new SysKnowledgeBaseDocumentChunk();
        entity.setDocumentId(doc.getDocumentId());
        entity.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        entity.setChunkIndex(chunk.getChunkIndex());
        entity.setContent(chunk.getContent());
        entity.setFileName(doc.getName());
        entity.setVectorStatus("0");
        entity.setMetadata(doc.getMetadata());
        return entity;
    }

    private IChatRequest buildChatRequest(SysModelConfigVo modelConfig, String promptTemplate,
                                         String content) {
        IChatRequest request = new IChatRequest();
        request.setCode(modelConfig.getModelCode());
        request.setModel(modelConfig.getModelCode());
        request.setBaseUrl(modelConfig.getBaseUrl());
        request.setApiKey(modelConfig.getApiKey());
        request.setStream(Boolean.TRUE);
        request.setPrompt(promptTemplate.replace("{input}", content));

        ResponseFormatRequest responseFormat = ResponseFormatRequest.builder()
            .type(ResponseFormatRequest.Type.JSON_OBJECT)
            .build();
        request.setResponseFormat(responseFormat);

        return request;
    }

    private Map<String, Object> buildEsDocument(SysKnowledgeBaseDocumentChunk chunk,
                                               SysKnowledgeBaseDocument doc,
                                               String title, String content,
                                               List<Float> embedding, String createTime) {
        Map<String, Object> esDoc = new HashMap<>();
        esDoc.put("documentId", chunk.getDocumentId().toString());
        esDoc.put("chunkId", chunk.getChunkId().toString() + "_" + IdUtil.fastSimpleUUID());
        esDoc.put("knowledgeBaseId", doc.getKnowledgeBaseId());
        esDoc.put("chunkTitle", title);
        esDoc.put("content", content);
        esDoc.put("fileName", chunk.getFileName());
        esDoc.put("metadata", chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
        esDoc.put("createTime", createTime);
        esDoc.put("embedding", embedding);
        return esDoc;
    }

    private void updateDocumentStatus(Long documentId, String status) {
        batchOperationHelper.batchUpdateDocumentStatus(
            Collections.singletonList(documentId), status);
    }

    private int processParseResponseAndUpdate(List<SysKnowledgeBaseDocument> documents,
                                             DocumentParseResponse response) {
        if (response == null || response.getSuccessTaskIds() == null) {
            SnailJobLog.REMOTE.error("文档解析API调用失败或返回空响应");
            return 0;
        }

        Map<Long, Map<String, Object>> updates = new ConcurrentHashMap<>();

        for (Map.Entry<String, String> entry : response.getSuccessTaskIds().entrySet()) {
            String fileUrl = entry.getKey();
            String taskId = entry.getValue();

            documents.stream()
                .filter(d -> fileUrl.equals(d.getUrl()))
                .findFirst()
                .ifPresent(doc -> {
                    Map<String, Object> fields = new HashMap<>();
                    fields.put("status", SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING);
                    fields.put("taskId", taskId);
                    updates.put(doc.getDocumentId(), fields);
                    SnailJobLog.REMOTE.info("文档 {} 分配任务ID: {}", doc.getName(), taskId);
                });
        }

        return batchOperationHelper.batchUpdateDocuments(updates);
    }

    private DocumentParseResponse callDocumentParseApi(List<DocumentParseRequest> requests) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<List<DocumentParseRequest>> entity = new HttpEntity<>(requests, headers);

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

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("调用文档解析API失败", e);
            return null;
        }
    }

    private LinkedHashMap<String, String> parseJsonToFlatFormat(JSONObject json) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        parseJsonRecursive(json, "", result);
        return result;
    }

    private void parseJsonRecursive(Object obj, String currentPath,
                                   LinkedHashMap<String, String> result) {
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            for (Map.Entry<String, Object> entry : jsonObj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String newPath = currentPath.isEmpty() ? key : currentPath + "," + key;
                parseJsonRecursive(value, newPath, result);
            }
        } else {
            String leafValue = obj != null ? obj.toString() : "";
            String pathWithValue = currentPath + "/n" + leafValue;
            result.put(currentPath, pathWithValue);
            result.put(pathWithValue, pathWithValue);
        }
    }
}
