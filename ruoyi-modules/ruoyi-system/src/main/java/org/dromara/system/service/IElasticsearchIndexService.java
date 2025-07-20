package org.dromara.system.service;

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
}