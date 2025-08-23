package org.dromara.system.service;

import java.util.List;
import java.util.Map;

/**
 * Elasticsearch index management service interface.
 * Provides operations for managing knowledge base indices in Elasticsearch.
 *
 * @author ruoyi
 */
public interface IElasticsearchIndexService {

    /**
     * Creates an index for a knowledge base with predefined mapping.
     * The index will include fields for documentId, content, fileName, metadata, createTime, and embedding.
     *
     * @param knowledgeBaseName the name of the knowledge base, used as index name
     * @return true if index creation succeeded, false otherwise
     */
    Boolean createKnowledgeBaseIndex(String knowledgeBaseName);

    /**
     * Checks if an index exists for the given knowledge base.
     *
     * @param knowledgeBaseName the name of the knowledge base
     * @return true if index exists, false otherwise
     */
    Boolean indexExists(String knowledgeBaseName);

    /**
     * Deletes an index for the given knowledge base.
     *
     * @param knowledgeBaseName the name of the knowledge base
     * @return true if deletion succeeded, false otherwise
     */
    Boolean deleteKnowledgeBaseIndex(String knowledgeBaseName);

    /**
     * Generates the index name for a knowledge base.
     * Converts knowledge base name to a valid Elasticsearch index name with environment prefix.
     * Format: knowledge_base_{environment}_{sanitized_name}
     *
     * @param knowledgeBaseName the name of the knowledge base
     * @return the formatted index name with environment information
     */
    String generateIndexName(String knowledgeBaseName);

    /**
     * Batch store documents to Elasticsearch.
     *
     * @param indexName the index name to store documents
     * @param documents a list of documents, each containing id and document data
     * @return the number of successfully stored documents
     */
    int batchStoreDocuments(String indexName, List<Map<String, Object>> documents);

    /**
     * Batch deletes indices for multiple knowledge bases.
     *
     * @param knowledgeBaseNames list of knowledge base names whose indices should be deleted
     * @return true if all deletions succeeded, false if any deletion failed
     */
    Boolean batchDeleteKnowledgeBaseIndices(List<Long> knowledgeBaseNames);
}
