package org.dromara.job.snailjob;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.job.helper.BatchOperationHelper;
import org.dromara.system.constant.SysKnowledgeBaseDocumentConstants;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.service.IDocumentSplitService;
import org.dromara.system.service.IEmbeddingService;
import org.dromara.system.service.IElasticsearchIndexService;
import org.dromara.system.service.split.DocumentChunk;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 优化后的知识库Excel文档处理定时任务
 * 使用线程池并行处理，提高处理效率
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseExcelDocumentTaskOptimized {

    @Qualifier("documentProcessingExecutor")
    private final ThreadPoolTaskExecutor documentProcessingExecutor;

    @Qualifier("embeddingExecutor")
    private final ThreadPoolTaskExecutor embeddingExecutor;

    private final SysKnowledgeBaseDocumentMapper documentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper chunkMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;
    private final BatchOperationHelper batchOperationHelper;
    private final IDocumentSplitService documentSplitService;
    private final IEmbeddingService embeddingService;
    private final IElasticsearchIndexService elasticsearchIndexService;

    @Value("${excel.processing.batch-size:10}")
    private int batchSize;

    @Value("${excel.processing.embedding.batch-size:20}")
    private int embeddingBatchSize;

    private static final int MAX_CONTENT_LENGTH = 10000;
    private static final String VECTOR_STATUS_PENDING = "0";
    private static final String VECTOR_STATUS_PROCESSING = "1";
    private static final String VECTOR_STATUS_COMPLETED = "2";
    private static final String VECTOR_STATUS_FAILED = "3";

    /**
     * Excel解析和分块任务 - 优化版
     * 并行处理Excel文档，提高处理效率
     */
    @JobExecutor(name = "excelParsingJobOptimized")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult excelParsingJobOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版Excel解析任务");

            // 批量获取待处理的Excel文档
            List<SysKnowledgeBaseDocument> documents = fetchExcelDocumentsToProcess(batchSize);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待处理的Excel文档");
                return ExecuteResult.success("没有待处理的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理的Excel文档", documents.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 并行处理Excel文档
            List<CompletableFuture<Void>> futures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(() -> {
                    try {
                        if (processExcelDocumentOptimized(doc)) {
                            successCount.incrementAndGet();
                            SnailJobLog.REMOTE.info("成功处理Excel文档: {}", doc.getName());
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        SnailJobLog.REMOTE.error("处理Excel文档 {} 时发生错误", doc.getName(), e);
                        updateDocumentStatus(doc.getDocumentId(),
                            SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FAIL);
                    }
                }, documentProcessingExecutor))
                .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            SnailJobLog.REMOTE.info("Excel解析任务完成: 成功 {} 个, 失败 {} 个",
                successCount.get(), failCount.get());
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个",
                successCount.get(), failCount.get()));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("Excel解析任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * Excel文档向量化任务 - 优化版
     * 多级并行处理：文档级并行 + 批量向量化
     */
    @JobExecutor(name = "excelEmbeddingJobOptimized")
    public ExecuteResult excelEmbeddingJobOptimized(JobArgs jobArgs) {
        try {
            SnailJobLog.REMOTE.info("开始执行优化版Excel向量化任务");

            // 批量获取待向量化的Excel文档
            List<SysKnowledgeBaseDocument> documents = fetchExcelEmbeddingDocuments(batchSize);
            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有待向量化的Excel文档");
                return ExecuteResult.success("没有待向量化的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待向量化的Excel文档", documents.size());

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            // 文档级并行处理
            List<CompletableFuture<Void>> documentFutures = documents.stream()
                .map(doc -> CompletableFuture.runAsync(() -> {
                    try {
                        if (processExcelDocumentEmbedding(doc)) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        SnailJobLog.REMOTE.error("处理Excel文档 {} 向量化时发生错误", doc.getName(), e);
                    }
                }, embeddingExecutor))
                .collect(Collectors.toList());

            // 等待所有文档处理完成
            CompletableFuture.allOf(documentFutures.toArray(new CompletableFuture[0])).join();

            SnailJobLog.REMOTE.info("Excel向量化任务完成: 成功 {} 个, 失败 {} 个",
                successCount.get(), failCount.get());
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个",
                successCount.get(), failCount.get()));

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("Excel向量化任务执行失败", e);
            return ExecuteResult.failure(e.getMessage());
        }
    }

    /**
     * 处理单个Excel文档的解析和分块
     */
    private boolean processExcelDocumentOptimized(SysKnowledgeBaseDocument document) {
        try {
            SnailJobLog.REMOTE.info("开始处理Excel文档: {}", document.getName());

            // 验证文档状态，防止重复处理
            if (!SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED.equals(document.getStatus())) {
                SnailJobLog.REMOTE.warn("文档 {} 状态不是待执行，跳过处理. 当前状态: {}",
                    document.getName(), document.getStatus());
                return false;
            }

            // 原子性更新状态为解析中
            boolean statusUpdated = updateDocumentStatusWithCheck(document.getDocumentId(),
                SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING);

            if (!statusUpdated) {
                SnailJobLog.REMOTE.warn("文档 {} 状态更新失败，可能已被其他任务处理", document.getName());
                return false;
            }

            // 获取文档URL并验证
            String documentUrl = document.getUrl();
            if (StrUtil.isBlank(documentUrl)) {
                throw new IllegalArgumentException("文档URL为空");
            }

            // Excel文档按行处理，每行作为一个完整的块
            List<DocumentChunk> chunks = documentSplitService.splitDocument(
                documentUrl, document.getType(), 1, 0);

            if (CollUtil.isEmpty(chunks)) {
                SnailJobLog.REMOTE.warn("文档 {} 未产生任何切分块", document.getName());
                updateDocumentStatus(document.getDocumentId(),
                    SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);
                return false;
            }

            SnailJobLog.REMOTE.info("文档 {} 切分为 {} 个块", document.getName(), chunks.size());

            // 清理可能存在的旧切块数据
            cleanupExistingChunks(document.getDocumentId());

            // 构建chunk实体列表
            List<SysKnowledgeBaseDocumentChunk> chunkEntities = chunks.stream()
                .map(chunk -> buildChunkEntity(chunk, document))
                .collect(Collectors.toList());

            // 批量插入chunks
            int insertedCount = batchOperationHelper.batchInsertChunks(chunkEntities);

            if (insertedCount > 0) {
                // 更新文档状态为待向量化
                updateDocumentStatus(document.getDocumentId(),
                    SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);
                SnailJobLog.REMOTE.info("文档 {} 分块成功，生成 {} 个块",
                    document.getName(), insertedCount);
                return true;
            }

            return false;

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("处理Excel文档 {} 时发生错误", document.getName(), e);
            throw new RuntimeException("处理文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理单个Excel文档的向量化
     */
    private boolean processExcelDocumentEmbedding(SysKnowledgeBaseDocument doc) {
        try {
            // 检查知识库是否有效
            if (!isKnowledgeBaseValid(doc.getKnowledgeBaseId())) {
                SnailJobLog.REMOTE.warn("知识库不存在或已删除，跳过文档处理: documentId={}, knowledgeBaseId={}",
                    doc.getDocumentId(), doc.getKnowledgeBaseId());
                updateDocumentStatus(doc.getDocumentId(),
                    SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FAIL);
                return false;
            }

            // 获取待处理的chunks
            List<SysKnowledgeBaseDocumentChunk> chunks = fetchPendingChunks(doc.getDocumentId());
            if (CollUtil.isEmpty(chunks)) {
                SnailJobLog.REMOTE.info("文档 {} 没有待处理的分块", doc.getName());
                // 检查是否所有块都已完成
                if (batchOperationHelper.checkAllChunksCompleted(doc.getDocumentId())) {
                    updateDocumentStatus(doc.getDocumentId(),
                        SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FINISHED);
                    return true;
                }
                return false;
            }

            SnailJobLog.REMOTE.info("文档 {} 找到 {} 个待向量化分块", doc.getName(), chunks.size());

            // 批量更新状态为处理中
            List<Long> chunkIds = chunks.stream()
                .map(SysKnowledgeBaseDocumentChunk::getChunkId)
                .collect(Collectors.toList());
            batchOperationHelper.batchUpdateChunkStatus(chunkIds, VECTOR_STATUS_PROCESSING);

            // 生成ES索引名
            String indexName = elasticsearchIndexService.generateIndexName(
                doc.getKnowledgeBaseId().toString());

            // 分批处理chunks
            List<List<SysKnowledgeBaseDocumentChunk>> chunkBatches =
                Lists.partition(chunks, embeddingBatchSize);

            List<CompletableFuture<List<Map<String, Object>>>> embeddingFutures =
                chunkBatches.stream()
                    .map(batch -> CompletableFuture.supplyAsync(() ->
                        processExcelChunkBatch(batch, doc, indexName),
                        documentProcessingExecutor))
                    .collect(Collectors.toList());

            // 等待所有批次完成
            CompletableFuture.allOf(embeddingFutures.toArray(new CompletableFuture[0])).join();

            // 收集所有ES文档
            List<Map<String, Object>> allEsDocuments = embeddingFutures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

            // 批量存储到ES
            if (!allEsDocuments.isEmpty()) {
                int storedCount = elasticsearchIndexService.batchStoreDocuments(indexName, allEsDocuments);
                SnailJobLog.REMOTE.info("文档 {} 批量索引完成，成功存储 {}/{} 个文档到ES",
                    doc.getName(), storedCount, allEsDocuments.size());

                if (storedCount != allEsDocuments.size()) {
                    SnailJobLog.REMOTE.warn("部分文档存储失败，尝试重新处理失败的文档");
                }
            }

            // 批量更新chunk状态为完成
            batchOperationHelper.batchUpdateChunkStatus(chunkIds, VECTOR_STATUS_COMPLETED);

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
     * 批量处理Excel chunks的向量化
     */
    private List<Map<String, Object>> processExcelChunkBatch(
            List<SysKnowledgeBaseDocumentChunk> chunks,
            SysKnowledgeBaseDocument doc,
            String indexName) {

        List<Map<String, Object>> esDocuments = new ArrayList<>();

        // 批量获取文本内容
        List<String> contents = chunks.stream()
            .map(chunk -> {
                String content = chunk.getContent();
                // 限制内容长度
                if (content != null && content.length() > MAX_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_CONTENT_LENGTH);
                    SnailJobLog.REMOTE.warn("文档切块内容过长已截断: chunkId={}, originalLength={}",
                        chunk.getChunkId(), chunk.getContent().length());
                }
                return content;
            })
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toList());

        if (CollUtil.isEmpty(contents)) {
            SnailJobLog.REMOTE.warn("批次中没有有效的内容需要向量化");
            return esDocuments;
        }

        try {
            // 批量调用embedding服务
            List<float[]> embeddings = new ArrayList<>();
            for (String content : contents) {
                float[] embedding = embeddingService.textToEmbeddingArray(content);
                if (embedding != null && embedding.length > 0) {
                    embeddings.add(embedding);
                } else {
                    embeddings.add(new float[0]); // 添加空数组保持索引对齐
                }
            }

            // 构建ES文档
            String createTime = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            for (int i = 0; i < chunks.size() && i < embeddings.size(); i++) {
                SysKnowledgeBaseDocumentChunk chunk = chunks.get(i);
                float[] embedding = embeddings.get(i);

                if (embedding.length == 0) {
                    SnailJobLog.REMOTE.warn("切块 {} 向量化失败，跳过", chunk.getChunkId());
                    continue;
                }

                Map<String, Object> esDoc = buildEsDocument(chunk, doc, embedding, createTime);
                esDocuments.add(esDoc);
            }

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("批量处理Excel chunks向量化失败", e);
            // 降级为单个处理
            for (SysKnowledgeBaseDocumentChunk chunk : chunks) {
                try {
                    String content = chunk.getContent();
                    if (StrUtil.isBlank(content)) {
                        continue;
                    }

                    if (content.length() > MAX_CONTENT_LENGTH) {
                        content = content.substring(0, MAX_CONTENT_LENGTH);
                    }

                    float[] embedding = embeddingService.textToEmbeddingArray(content);
                    if (embedding != null && embedding.length > 0) {
                        String createTime = LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        Map<String, Object> esDoc = buildEsDocument(chunk, doc, embedding, createTime);
                        esDocuments.add(esDoc);
                    }
                } catch (Exception ex) {
                    SnailJobLog.REMOTE.error("处理单个chunk {} 向量化失败", chunk.getChunkId(), ex);
                }
            }
        }

        return esDocuments;
    }

    // ========== 辅助方法 ==========

    /**
     * 查询待处理的Excel文档
     */
    private List<SysKnowledgeBaseDocument> fetchExcelDocumentsToProcess(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED)
            .in(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    /**
     * 查询待向量化的Excel文档
     */
    private List<SysKnowledgeBaseDocument> fetchExcelEmbeddingDocuments(int limit) {
        Page<SysKnowledgeBaseDocument> page = new Page<>(1, limit);
        LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocument::getStatus,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING)
            .in(SysKnowledgeBaseDocument::getType, "xlsx", "xls")
            .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

        return documentMapper.selectPage(page, queryWrapper).getRecords();
    }

    /**
     * 查询待处理的文档块
     */
    private List<SysKnowledgeBaseDocumentChunk> fetchPendingChunks(Long documentId) {
        LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> queryWrapper =
            new LambdaQueryWrapper<>();
        queryWrapper.eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId)
            .eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, VECTOR_STATUS_PENDING)
            .orderByAsc(SysKnowledgeBaseDocumentChunk::getChunkIndex);

        return chunkMapper.selectList(queryWrapper);
    }

    /**
     * 构建chunk实体
     */
    private SysKnowledgeBaseDocumentChunk buildChunkEntity(DocumentChunk chunk,
                                                          SysKnowledgeBaseDocument doc) {
        SysKnowledgeBaseDocumentChunk entity = new SysKnowledgeBaseDocumentChunk();
        entity.setDocumentId(doc.getDocumentId());
        entity.setKnowledgeBaseId(doc.getKnowledgeBaseId());
        entity.setChunkIndex(chunk.getChunkIndex());
        entity.setContent(chunk.getContent());
        entity.setFileName(doc.getName());
        entity.setVectorStatus(VECTOR_STATUS_PENDING);
        entity.setMetadata(chunk.getMetadata() != null ? chunk.getMetadata() : doc.getMetadata());
        return entity;
    }

    /**
     * 构建ES文档
     */
    private Map<String, Object> buildEsDocument(SysKnowledgeBaseDocumentChunk chunk,
                                               SysKnowledgeBaseDocument doc,
                                               float[] embedding,
                                               String createTime) {
        Map<String, Object> esDoc = new HashMap<>();
        esDoc.put("documentId", chunk.getDocumentId().toString());
        esDoc.put("chunkId", chunk.getChunkId().toString() + "_" + IdUtil.fastSimpleUUID());
        esDoc.put("knowledgeBaseId", doc.getKnowledgeBaseId());
        esDoc.put("content", chunk.getContent());
        esDoc.put("chunkTitle", chunk.getContent());
        esDoc.put("fileName", chunk.getFileName());
        esDoc.put("metadata", chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
        esDoc.put("createTime", createTime);
        esDoc.put("embedding", embedding);
        return esDoc;
    }

    /**
     * 清理已存在的chunks
     */
    private void cleanupExistingChunks(Long documentId) {
        try {
            LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId);

            long existingCount = chunkMapper.selectCount(queryWrapper);
            if (existingCount > 0) {
                int deletedCount = chunkMapper.delete(queryWrapper);
                SnailJobLog.REMOTE.info("清理文档 {} 的 {} 个已存在切块", documentId, deletedCount);
            }
        } catch (Exception e) {
            SnailJobLog.REMOTE.warn("清理文档 {} 已存在切块时发生错误: {}", documentId, e.getMessage());
            // 不抛出异常，允许继续处理
        }
    }

    /**
     * 更新文档状态
     */
    private void updateDocumentStatus(Long documentId, String status) {
        batchOperationHelper.batchUpdateDocumentStatus(
            Collections.singletonList(documentId), status);
    }

    /**
     * 带条件检查的状态更新
     */
    private boolean updateDocumentStatusWithCheck(Long documentId, String expectedStatus, String newStatus) {
        Map<Long, Map<String, Object>> updates = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", newStatus);
        updates.put(documentId, fields);

        // 先检查当前状态
        SysKnowledgeBaseDocument doc = documentMapper.selectById(documentId);
        if (doc != null && expectedStatus.equals(doc.getStatus())) {
            int result = batchOperationHelper.batchUpdateDocuments(updates);
            return result > 0;
        }

        return false;
    }

    /**
     * 检查知识库是否有效
     */
    private boolean isKnowledgeBaseValid(Long knowledgeBaseId) {
        try {
            if (knowledgeBaseId == null) {
                SnailJobLog.REMOTE.warn("知识库ID为空");
                return false;
            }

            SysKnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
            if (knowledgeBase == null) {
                SnailJobLog.REMOTE.warn("知识库不存在: knowledgeBaseId={}", knowledgeBaseId);
                return false;
            }

            // 检查删除标志
            if (!"0".equals(knowledgeBase.getDelFlag())) {
                SnailJobLog.REMOTE.warn("知识库已删除: knowledgeBaseId={}, delFlag={}",
                    knowledgeBaseId, knowledgeBase.getDelFlag());
                return false;
            }

            SnailJobLog.REMOTE.debug("知识库状态正常: knowledgeBaseId={}", knowledgeBaseId);
            return true;

        } catch (Exception e) {
            SnailJobLog.REMOTE.error("检查知识库状态失败: knowledgeBaseId={}", knowledgeBaseId, e);
            return false;
        }
    }
}
