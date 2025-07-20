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
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.IDocumentSplitService;
import org.dromara.system.service.split.DocumentChunk;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseDocumentTask {

    private final SysKnowledgeBaseDocumentMapper sysKnowledgeBaseDocumentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper sysKnowledgeBaseDocumentChunkMapper;
    private final IDocumentSplitService documentSplitService;

    private static final int BATCH_SIZE = 10;
    private static final int CHUNK_BATCH_SIZE = 100; // 每批次插入的切块数量
    private static final String VECTOR_STATUS_PENDING = "0";
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


//    @JobExecutor(name = "contentEmbeddingJob")
//    @Transactional(rollbackFor = Exception.class)
//    public ExecuteResult contentEmbeddingJob(JobArgs jobArgs) {
//        // 1.从 SysKnowledgeBaseDocumentChunkMapper 中查询到未开始向量化的任务
//        // 2. 将向量话状态改为待向量化
//        // 3. 调用embedding 接口，将数据向量化
//        // 4. 将数据写入es 数据库
//        // 5. 将向量化状态修改为已向量化
//    }

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

}
