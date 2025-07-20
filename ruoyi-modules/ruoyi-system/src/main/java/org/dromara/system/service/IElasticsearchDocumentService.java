package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.vo.SysKnowledgeBaseEsDocumentVo;

/**
 * Elasticsearch document management service interface.
 * Provides operations for querying and managing documents in Elasticsearch indices.
 *
 * @author ruoyi
 */
public interface IElasticsearchDocumentService {

    /**
     * Queries documents from Elasticsearch by document ID with pagination.
     * Excludes embedding fields from the result to optimize response size.
     *
     * @param documentId the document ID to search for
     * @param pageQuery  pagination parameters
     * @return paginated list of documents without embedding fields
     */
    TableDataInfo<SysKnowledgeBaseEsDocumentVo> queryDocumentsByDocumentId(Long documentId, PageQuery pageQuery);

    /**
     * Deletes documents from Elasticsearch by document ID.
     * This will remove all chunks associated with the given document ID.
     *
     * @param documentId the document ID to delete
     * @return true if deletion succeeded, false otherwise
     */
    Boolean deleteDocumentsByDocumentId(Long documentId);

    /**
     * Deletes a specific chunk document by chunk ID.
     *
     * @param documentId the document ID (used to determine index)
     * @param chunkId    the chunk ID to delete
     * @return true if deletion succeeded, false otherwise
     */
    Boolean deleteChunkById(Long documentId, Long chunkId);

    /**
     * Deletes a specific chunk document by chunk ID only.
     * This method will automatically find the document ID and index.
     *
     * @param chunkId the chunk ID to delete
     * @return true if deletion succeeded, false otherwise
     */
    Boolean deleteChunkByChunkId(String chunkId);
}