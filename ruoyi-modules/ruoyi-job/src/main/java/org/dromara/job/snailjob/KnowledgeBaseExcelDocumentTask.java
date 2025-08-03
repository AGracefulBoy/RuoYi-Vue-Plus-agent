package org.dromara.job.snailjob;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.client.model.ExecuteResult;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
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
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseExcelDocumentTask {

    private final SysKnowledgeBaseDocumentMapper sysKnowledgeBaseDocumentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper sysKnowledgeBaseDocumentChunkMapper;
    private final SysKnowledgeBaseMapper sysKnowledgeBaseMapper;
    private final IDocumentSplitService documentSplitService;
    private final IEmbeddingService embeddingService;
    private final IElasticsearchIndexService elasticsearchIndexService;
    private final ElasticsearchClient elasticsearchClient;

    private static final int BATCH_SIZE = 10;
    private static final int CHUNK_BATCH_SIZE = 100; // 每批次插入的切块数量
    private static final int EMBEDDING_BATCH_SIZE = 20; // 每批次向量化的数量
    private static final String VECTOR_STATUS_PENDING = "0";
    private static final String VECTOR_STATUS_PROCESSING = "1";
    private static final String VECTOR_STATUS_COMPLETED = "2";
    private static final String VECTOR_STATUS_FAILED = "3";
    private static final int CHUNK_SIZE = 1000;
    private static final int OVERLAP_SIZE = 100;
    private static final int MAX_CONTENT_LENGTH = 10000; // 最大内容长度限制

    @JobExecutor(name = "excelParsingJob")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult excelParsingJob(JobArgs jobArgs) throws InterruptedException {
        try {
            SnailJobLog.REMOTE.info("开始执行 Excel 解析任务");

            // 1. 分页查询待执行的 Excel 文件，每次处理 10 个
            Page<SysKnowledgeBaseDocument> page = new Page<>(1, BATCH_SIZE);
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getStatus, SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED)
                       .in(SysKnowledgeBaseDocument::getType, "excel")
                       .orderByAsc(SysKnowledgeBaseDocument::getCreateTime);

            Page<SysKnowledgeBaseDocument> documentPage = sysKnowledgeBaseDocumentMapper.selectPage(page, queryWrapper);
            List<SysKnowledgeBaseDocument> documents = documentPage.getRecords();

            if (CollUtil.isEmpty(documents)) {
                SnailJobLog.REMOTE.info("没有找到待处理的 Excel 文档");
                return ExecuteResult.success("没有待处理的文档");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待处理的 Excel 文档", documents.size());

            int successCount = 0;
            int failCount = 0;

            for (SysKnowledgeBaseDocument document : documents) {
                try {
                    processExcelDocument(document);
                    successCount++;
                    SnailJobLog.REMOTE.info("成功处理文档: {}", document.getName());
                } catch (Exception exception) {
                    failCount++;
                    SnailJobLog.REMOTE.error("处理文档失败: {}, 错误: {}", document.getName(), exception.getMessage());

                    // 更新文档状态为失败
                    updateDocumentStatus(document.getDocumentId(), SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_FAIL);
                }
            }

            SnailJobLog.REMOTE.info("Excel 解析任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("Excel 解析任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }
    }


    @JobExecutor(name = "contentEmbeddingJob")
    @Transactional(rollbackFor = Exception.class)
    public ExecuteResult contentEmbeddingJob(JobArgs jobArgs) throws InterruptedException {
        try {
            SnailJobLog.REMOTE.info("开始执行内容向量化任务");

            // 1. 分页查询待向量化的文档块
            Page<SysKnowledgeBaseDocumentChunk> page = new Page<>(1, EMBEDDING_BATCH_SIZE);
            LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, VECTOR_STATUS_PENDING)
                       .orderByAsc(SysKnowledgeBaseDocumentChunk::getCreateTime);

            Page<SysKnowledgeBaseDocumentChunk> chunkPage = sysKnowledgeBaseDocumentChunkMapper.selectPage(page, queryWrapper);
            List<SysKnowledgeBaseDocumentChunk> chunks = chunkPage.getRecords();

            if (CollUtil.isEmpty(chunks)) {
                SnailJobLog.REMOTE.info("没有找到待向量化的文档块");
                return ExecuteResult.success("没有待处理的文档块");
            }

            SnailJobLog.REMOTE.info("找到 {} 个待向量化的文档块", chunks.size());

            int successCount = 0;
            int failCount = 0;

            for (SysKnowledgeBaseDocumentChunk chunk : chunks) {
                try {
                    processChunkEmbedding(chunk);
                    successCount++;
                    SnailJobLog.REMOTE.debug("成功处理文档块: chunkId={}", chunk.getChunkId());
                } catch (Exception exception) {
                    failCount++;
                    SnailJobLog.REMOTE.error("处理文档块失败: chunkId={}, 错误: {}",
                        chunk.getChunkId(), exception.getMessage());

                    // 更新文档块状态为失败
                    updateChunkVectorStatus(chunk.getChunkId(), VECTOR_STATUS_FAILED);
                }
            }

            SnailJobLog.REMOTE.info("内容向量化任务完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
            return ExecuteResult.success(String.format("处理完成: 成功 %d 个, 失败 %d 个", successCount, failCount));

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("内容向量化任务执行失败", exception);
            return ExecuteResult.failure(exception.getMessage());
        }
    }

    /**
     * Process single document chunk for embedding and ES storage.
     *
     * @param chunk the document chunk to process
     */
    private void processChunkEmbedding(SysKnowledgeBaseDocumentChunk chunk) {
        try {
            SnailJobLog.REMOTE.debug("开始处理文档块向量化: chunkId={}", chunk.getChunkId());

            // 1. 检查知识库状态，如果知识库不存在或已删除则跳过
            if (!isKnowledgeBaseValid(chunk.getKnowledgeBaseId())) {
                SnailJobLog.REMOTE.warn("知识库不存在或已删除，跳过文档块处理: chunkId={}, knowledgeBaseId={}",
                    chunk.getChunkId(), chunk.getKnowledgeBaseId());

                // 更新文档块状态为失败，避免重复处理
                updateChunkVectorStatus(chunk.getChunkId(), VECTOR_STATUS_FAILED);
                return;
            }

            // 2. 更新状态为处理中，防止重复处理
            boolean statusUpdated = updateChunkVectorStatusWithCheck(chunk.getChunkId(),
                VECTOR_STATUS_PENDING, VECTOR_STATUS_PROCESSING);

            if (!statusUpdated) {
                SnailJobLog.REMOTE.warn("文档块 {} 状态更新失败，可能已被其他任务处理", chunk.getChunkId());
                return;
            }

            // 2. 验证内容
            String content = chunk.getContent();
            if (StrUtil.isBlank(content)) {
                throw new IllegalArgumentException("文档块内容为空");
            }

            // 3. 调用embedding服务转换为向量
            float[] embedding = embeddingService.textToEmbeddingArray(content);
            if (embedding == null || embedding.length == 0) {
                throw new RuntimeException("向量化结果为空");
            }

            SnailJobLog.REMOTE.debug("成功获取向量，维度: {}", embedding.length);

            // 4. 生成ES索引名
            String indexName = elasticsearchIndexService.generateIndexName(chunk.getKnowledgeBaseId().toString());

            // 5. 构建ES文档数据
            Map<String, Object> esDocument = buildEsDocument(chunk, embedding);

            // 6. 存储到ES
            storeToElasticsearch(indexName, chunk.getChunkId().toString(), esDocument);

            // 7. 更新状态为已完成
            updateChunkVectorStatus(chunk.getChunkId(), VECTOR_STATUS_COMPLETED);

            SnailJobLog.REMOTE.debug("文档块向量化处理完成: chunkId={}", chunk.getChunkId());

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("处理文档块向量化失败: chunkId={}", chunk.getChunkId(), exception);
            throw new RuntimeException("处理文档块向量化失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * Process single Excel document with enhanced error handling and validation.
     *
     * @param document the document to process
     */
    private void processExcelDocument(SysKnowledgeBaseDocument document) {
        try {
            SnailJobLog.REMOTE.info("开始处理文档: {}", document.getName());

            // 验证文档状态，防止重复处理
            if (!SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED.equals(document.getStatus())) {
                SnailJobLog.REMOTE.warn("文档 {} 状态不是待执行，跳过处理. 当前状态: {}",
                    document.getName(), document.getStatus());
                return;
            }

            // 2. 原子性更新状态为解析中，防止并发处理
            boolean statusUpdated = updateDocumentStatusWithCheck(document.getDocumentId(),
                SysKnowledgeBaseDocumentConstants.STATUS_TO_BE_EXECUTED,
                SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_PARSING);

            if (!statusUpdated) {
                SnailJobLog.REMOTE.warn("文档 {} 状态更新失败，可能已被其他任务处理", document.getName());
                return;
            }

            // 3. 获取文档URL并调用Excel解析方法
            String documentUrl = document.getUrl();
            if (StrUtil.isBlank(documentUrl)) {
                throw new IllegalArgumentException("文档URL为空");
            }

            // 验证文档类型
            if (!documentSplitService.isDocumentTypeSupported(document.getType())) {
                throw new IllegalArgumentException("不支持的文档类型: " + document.getType());
            }

            // Excel 文档按行处理，不需要切分参数，每行作为一个完整的块存储
            List<DocumentChunk> chunks = documentSplitService.splitDocument(
                documentUrl, document.getType(), 1, 0);

            if (CollUtil.isEmpty(chunks)) {
                SnailJobLog.REMOTE.warn("文档 {} 未产生任何切分块", document.getName());
                updateDocumentStatus(document.getDocumentId(), SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);
                return;
            }

            SnailJobLog.REMOTE.info("文档 {} 切分为 {} 个块", document.getName(), chunks.size());

            // 4. 清理可能存在的旧切块数据（防止重复处理时数据重复）
            cleanupExistingChunks(document.getDocumentId());

            // 5. 批量写入文档切块到数据库
            batchInsertDocumentChunks(document, chunks);

            // 6. 更新文档状态为待向量化
            updateDocumentStatus(document.getDocumentId(), SysKnowledgeBaseDocumentConstants.STATUS_DOCUMENT_EMBEDDING);

            SnailJobLog.REMOTE.info("文档 {} 处理完成", document.getName());

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("处理文档 {} 时发生错误", document.getName(), exception);
            throw new RuntimeException("处理文档失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * Update document status with condition check to prevent concurrent processing.
     *
     * @param documentId  the document ID
     * @param expectedStatus the expected current status
     * @param newStatus   the new status to set
     * @return true if update was successful, false otherwise
     */
    private boolean updateDocumentStatusWithCheck(Long documentId, String expectedStatus, String newStatus) {
        LambdaUpdateWrapper<SysKnowledgeBaseDocument> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysKnowledgeBaseDocument::getDocumentId, documentId)
                    .eq(SysKnowledgeBaseDocument::getStatus, expectedStatus)
                    .set(SysKnowledgeBaseDocument::getStatus, newStatus);

        int updateCount = sysKnowledgeBaseDocumentMapper.update(null, updateWrapper);
        boolean success = updateCount > 0;

        SnailJobLog.REMOTE.debug("条件更新文档状态: documentId={}, expectedStatus={}, newStatus={}, success={}",
            documentId, expectedStatus, newStatus, success);

        return success;
    }

    /**
     * Clean up existing chunks for a document to prevent duplicates.
     *
     * @param documentId the document ID
     */
    private void cleanupExistingChunks(Long documentId) {
        try {
            LambdaQueryWrapper<SysKnowledgeBaseDocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId);

            long existingCount = sysKnowledgeBaseDocumentChunkMapper.selectCount(queryWrapper);
            if (existingCount > 0) {
                int deletedCount = sysKnowledgeBaseDocumentChunkMapper.delete(queryWrapper);
                SnailJobLog.REMOTE.info("清理文档 {} 的 {} 个已存在切块", documentId, deletedCount);
            }
        } catch (Exception exception) {
            SnailJobLog.REMOTE.warn("清理文档 {} 已存在切块时发生错误: {}", documentId, exception.getMessage());
            // 不抛出异常，允许继续处理
        }
    }

    /**
     * Update document status.
     *
     * @param documentId the document ID
     * @param status     the new status
     */
    private void updateDocumentStatus(Long documentId, String status) {
        LambdaUpdateWrapper<SysKnowledgeBaseDocument> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysKnowledgeBaseDocument::getDocumentId, documentId)
                    .set(SysKnowledgeBaseDocument::getStatus, status);

        int updateCount = sysKnowledgeBaseDocumentMapper.update(null, updateWrapper);
        if (updateCount == 0) {
            throw new RuntimeException("更新文档状态失败: documentId=" + documentId);
        }

        SnailJobLog.REMOTE.debug("更新文档状态: documentId={}, status={}", documentId, status);
    }

    /**
     * Batch insert document chunks for performance optimization.
     * Uses chunked batch processing to handle large datasets efficiently.
     *
     * @param document the source document
     * @param chunks   the document chunks to insert
     */
    private void batchInsertDocumentChunks(SysKnowledgeBaseDocument document, List<DocumentChunk> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            return;
        }

        List<SysKnowledgeBaseDocumentChunk> chunkEntities = new ArrayList<>();
        int filteredCount = 0;

        for (DocumentChunk chunk : chunks) {
            // 验证和过滤内容
            String content = chunk.getContent();
            if (StrUtil.isBlank(content)) {
                filteredCount++;
                continue;
            }

            // 限制内容长度，防止数据库存储问题
            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
                SnailJobLog.REMOTE.warn("文档切块内容过长已截断: chunkIndex={}, originalLength={}",
                    chunk.getChunkIndex(), chunk.getContent().length());
            }

            SysKnowledgeBaseDocumentChunk chunkEntity = new SysKnowledgeBaseDocumentChunk();
            chunkEntity.setDocumentId(document.getDocumentId());
            chunkEntity.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunkEntity.setChunkIndex(chunk.getChunkIndex());
            chunkEntity.setContent(content);
            chunkEntity.setFileName(document.getName());
            chunkEntity.setVectorStatus(VECTOR_STATUS_PENDING); // 0 待处理
            chunkEntity.setMetadata(chunk.getMetadata());

            chunkEntities.add(chunkEntity);
        }

        if (filteredCount > 0) {
            SnailJobLog.REMOTE.info("过滤了 {} 个空内容切块", filteredCount);
        }

        if (CollUtil.isEmpty(chunkEntities)) {
            SnailJobLog.REMOTE.warn("所有切块都被过滤，没有数据插入");
            return;
        }

        // 分批插入，避免一次性插入过多数据导致内存或数据库性能问题
        int totalSize = chunkEntities.size();
        int insertedCount = 0;

        for (int i = 0; i < totalSize; i += CHUNK_BATCH_SIZE) {
            int endIndex = Math.min(i + CHUNK_BATCH_SIZE, totalSize);
            List<SysKnowledgeBaseDocumentChunk> batchChunks = chunkEntities.subList(i, endIndex);

            try {
                boolean insertResult = sysKnowledgeBaseDocumentChunkMapper.insertBatch(batchChunks);
                if (!insertResult) {
                    throw new RuntimeException("批量插入文档切块失败");
                }
                insertedCount += batchChunks.size();
                SnailJobLog.REMOTE.debug("成功插入第 {}/{} 批数据，共 {} 条",
                    (i / CHUNK_BATCH_SIZE + 1), (totalSize - 1) / CHUNK_BATCH_SIZE + 1, batchChunks.size());

            } catch (Exception exception) {
                SnailJobLog.REMOTE.error("批量插入第 {} 批数据失败", (i / CHUNK_BATCH_SIZE + 1), exception);
                throw new RuntimeException("批量插入文档切块失败: " + exception.getMessage(), exception);
            }
        }

        SnailJobLog.REMOTE.info("成功插入文档 {} 的所有切块，总计 {} 个", document.getName(), insertedCount);
    }

    /**
     * Update chunk vector status with condition check to prevent concurrent processing.
     *
     * @param chunkId        the chunk ID
     * @param expectedStatus the expected current status
     * @param newStatus      the new status to set
     * @return true if update was successful, false otherwise
     */
    private boolean updateChunkVectorStatusWithCheck(Long chunkId, String expectedStatus, String newStatus) {
        LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysKnowledgeBaseDocumentChunk::getChunkId, chunkId)
                    .eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, expectedStatus)
                    .set(SysKnowledgeBaseDocumentChunk::getVectorStatus, newStatus);

        int updateCount = sysKnowledgeBaseDocumentChunkMapper.update(null, updateWrapper);
        boolean success = updateCount > 0;

        SnailJobLog.REMOTE.debug("条件更新文档块向量状态: chunkId={}, expectedStatus={}, newStatus={}, success={}",
            chunkId, expectedStatus, newStatus, success);

        return success;
    }

    /**
     * Update chunk vector status.
     *
     * @param chunkId the chunk ID
     * @param status  the new vector status
     */
    private void updateChunkVectorStatus(Long chunkId, String status) {
        LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysKnowledgeBaseDocumentChunk::getChunkId, chunkId)
                    .set(SysKnowledgeBaseDocumentChunk::getVectorStatus, status);

        int updateCount = sysKnowledgeBaseDocumentChunkMapper.update(null, updateWrapper);
        if (updateCount == 0) {
            throw new RuntimeException("更新文档块向量状态失败: chunkId=" + chunkId);
        }

        SnailJobLog.REMOTE.debug("更新文档块向量状态: chunkId={}, status={}", chunkId, status);
    }

    /**
     * Build Elasticsearch document from chunk data.
     *
     * @param chunk     the document chunk
     * @param embedding the embedding vector
     * @return the ES document map
     */
    private Map<String, Object> buildEsDocument(SysKnowledgeBaseDocumentChunk chunk, float[] embedding) {
        Map<String, Object> esDocument = new HashMap<>();

        // Required fields according to ES mapping
        esDocument.put("documentId", chunk.getDocumentId().toString());
        esDocument.put("content", chunk.getContent());
        esDocument.put("fileName", chunk.getFileName());
        esDocument.put("metadata", chunk.getMetadata() != null ? chunk.getMetadata() : new HashMap<>());
        esDocument.put("createTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        esDocument.put("embedding", embedding);

        SnailJobLog.REMOTE.debug("构建ES文档: documentId={}, contentLength={}, embeddingDimension={}",
            chunk.getChunkId(), chunk.getContent().length(), embedding.length);

        return esDocument;
    }

    /**
     * Store document to Elasticsearch.
     *
     * @param indexName  the ES index name
     * @param documentId the document ID
     * @param document   the document data
     */
    private void storeToElasticsearch(String indexName, String documentId, Map<String, Object> document) {
        try {
            IndexRequest<Map<String, Object>> request = IndexRequest.of(builder -> builder
                .index(indexName)
                .id(documentId)
                .document(document)
            );

            elasticsearchClient.index(request);

            SnailJobLog.REMOTE.debug("成功存储文档到ES: index={}, documentId={}", indexName, documentId);

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("存储文档到ES失败: index={}, documentId={}", indexName, documentId, exception);
            throw new RuntimeException("存储文档到ES失败: " + exception.getMessage(), exception);
        }
    }

    /**
     * Check if knowledge base is valid (exists and not deleted).
     *
     * @param knowledgeBaseId the knowledge base ID
     * @return true if knowledge base is valid, false otherwise
     */
    private boolean isKnowledgeBaseValid(Long knowledgeBaseId) {
        try {
            if (knowledgeBaseId == null) {
                SnailJobLog.REMOTE.warn("知识库ID为空");
                return false;
            }

            SysKnowledgeBase knowledgeBase = sysKnowledgeBaseMapper.selectById(knowledgeBaseId);
            if (knowledgeBase == null) {
                SnailJobLog.REMOTE.warn("知识库不存在: knowledgeBaseId={}", knowledgeBaseId);
                return false;
            }

            // Check if del_flag equals 0 (not deleted)
            if (!"0".equals(knowledgeBase.getDelFlag())) {
                SnailJobLog.REMOTE.warn("知识库已删除: knowledgeBaseId={}, delFlag={}",
                    knowledgeBaseId, knowledgeBase.getDelFlag());
                return false;
            }

            SnailJobLog.REMOTE.debug("知识库状态正常: knowledgeBaseId={}", knowledgeBaseId);
            return true;

        } catch (Exception exception) {
            SnailJobLog.REMOTE.error("检查知识库状态失败: knowledgeBaseId={}", knowledgeBaseId, exception);
            return false;
        }
    }

}
