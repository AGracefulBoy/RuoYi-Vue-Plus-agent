package org.dromara.job.helper;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 批量操作辅助类
 * 提供文档和文档块的批量数据库操作
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchOperationHelper {

    private final SysKnowledgeBaseDocumentMapper documentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper chunkMapper;

    /**
     * 批量更新文档状态
     *
     * @param documentIds 文档ID列表
     * @param status      新状态
     * @return 更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDocumentStatus(List<Long> documentIds, String status) {
        if (CollUtil.isEmpty(documentIds)) {
            return 0;
        }

        return documentMapper.update(null,
            new LambdaUpdateWrapper<SysKnowledgeBaseDocument>()
                .in(SysKnowledgeBaseDocument::getDocumentId, documentIds)
                .set(SysKnowledgeBaseDocument::getStatus, status)
                .set(SysKnowledgeBaseDocument::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 批量更新文档状态和任务ID
     *
     * @param updates Map<documentId, Map<field, value>>
     * @return 成功更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateDocuments(Map<Long, Map<String, Object>> updates) {
        if (CollUtil.isEmpty(updates)) {
            return 0;
        }

        int successCount = 0;
        for (Map.Entry<Long, Map<String, Object>> entry : updates.entrySet()) {
            Long documentId = entry.getKey();
            Map<String, Object> fields = entry.getValue();

            LambdaUpdateWrapper<SysKnowledgeBaseDocument> wrapper = 
                new LambdaUpdateWrapper<SysKnowledgeBaseDocument>()
                    .eq(SysKnowledgeBaseDocument::getDocumentId, documentId);

            for (Map.Entry<String, Object> field : fields.entrySet()) {
                switch (field.getKey()) {
                    case "status":
                        wrapper.set(SysKnowledgeBaseDocument::getStatus, field.getValue());
                        break;
                    case "taskId":
                        wrapper.set(SysKnowledgeBaseDocument::getTaskId, field.getValue());
                        break;
                    case "parseCompletedUrl":
                        wrapper.set(SysKnowledgeBaseDocument::getParseCompletedUrl, field.getValue());
                        break;
                }
            }
            wrapper.set(SysKnowledgeBaseDocument::getUpdateTime, LocalDateTime.now());

            successCount += documentMapper.update(null, wrapper);
        }

        return successCount;
    }

    /**
     * 批量插入文档块
     *
     * @param chunks 文档块列表
     * @return 插入的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertChunks(List<SysKnowledgeBaseDocumentChunk> chunks) {
        if (CollUtil.isEmpty(chunks)) {
            return 0;
        }

        // 分批插入，每批1000条
        int batchSize = 1000;
        int totalInserted = 0;

        for (int i = 0; i < chunks.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, chunks.size());
            List<SysKnowledgeBaseDocumentChunk> batch = chunks.subList(i, endIndex);
            
            for (SysKnowledgeBaseDocumentChunk chunk : batch) {
                chunkMapper.insert(chunk);
                totalInserted++;
            }
        }

        return totalInserted;
    }

    /**
     * 批量更新文档块向量状态
     *
     * @param chunkIds 文档块ID列表
     * @param status   新状态
     * @return 更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchUpdateChunkStatus(List<Long> chunkIds, String status) {
        if (CollUtil.isEmpty(chunkIds)) {
            return 0;
        }

        return chunkMapper.update(null,
            new LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk>()
                .in(SysKnowledgeBaseDocumentChunk::getChunkId, chunkIds)
                .set(SysKnowledgeBaseDocumentChunk::getVectorStatus, status));
    }

    /**
     * 批量更新文档块向量状态（按文档ID）
     *
     * @param documentId 文档ID
     * @param oldStatus  旧状态
     * @param newStatus  新状态
     * @return 更新的记录数
     */
    @Transactional(rollbackFor = Exception.class)
    public int updateChunkStatusByDocument(Long documentId, String oldStatus, String newStatus) {
        return chunkMapper.update(null,
            new LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk>()
                .eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId)
                .eq(SysKnowledgeBaseDocumentChunk::getVectorStatus, oldStatus)
                .set(SysKnowledgeBaseDocumentChunk::getVectorStatus, newStatus));
    }

    /**
     * 检查文档的所有块是否都已完成向量化
     *
     * @param documentId 文档ID
     * @return true如果所有块都已完成
     */
    public boolean checkAllChunksCompleted(Long documentId) {
        Long pendingCount = chunkMapper.selectCount(
            new LambdaUpdateWrapper<SysKnowledgeBaseDocumentChunk>()
                .eq(SysKnowledgeBaseDocumentChunk::getDocumentId, documentId)
                .ne(SysKnowledgeBaseDocumentChunk::getVectorStatus, "2"));
        
        return pendingCount == 0;
    }
}