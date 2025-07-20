package org.dromara.system.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.IElasticsearchIndexService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Elasticsearch index management service implementation.
 * Handles creation, deletion, and management of knowledge base indices.
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ElasticsearchIndexServiceImpl implements IElasticsearchIndexService {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean createKnowledgeBaseIndex(String knowledgeBaseName) {
        try {
            String indexName = generateIndexName(knowledgeBaseName);
            
            // Check if index already exists
            if (indexExists(knowledgeBaseName)) {
                log.warn("Index already exists for knowledge base: {}", knowledgeBaseName);
                return true;
            }

            // Create index with mapping
            CreateIndexRequest request = CreateIndexRequest.of(builder -> builder
                .index(indexName)
                .mappings(mappings -> mappings
                    .properties("documentId", Property.of(p -> p
                        .keyword(k -> k)))
                    .properties("content", Property.of(p -> p
                        .text(t -> t
                            .analyzer("standard"))))
                    .properties("fileName", Property.of(p -> p
                        .keyword(k -> k)))
                    .properties("metadata", Property.of(p -> p
                        .object(o -> o
                            .enabled(true)
                            .dynamic(DynamicMapping.True))))
                    .properties("createTime", Property.of(p -> p
                        .date(d -> d
                            .format("yyyy-MM-dd HH:mm:ss||yyyy-MM-dd||epoch_millis"))))
                    .properties("embedding", Property.of(p -> p
                        .denseVector(v -> v
                            .dims(768)
                            .similarity("cosine"))))
                )
            );

            elasticsearchClient.indices().create(request);
            log.info("Successfully created index for knowledge base: {}", knowledgeBaseName);
            return true;

        } catch (Exception exception) {
            log.error("Failed to create index for knowledge base: {}", knowledgeBaseName, exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean indexExists(String knowledgeBaseName) {
        try {
            String indexName = generateIndexName(knowledgeBaseName);
            ExistsRequest request = ExistsRequest.of(builder -> builder
                .index(indexName));
            
            return elasticsearchClient.indices().exists(request).value();
        } catch (Exception exception) {
            log.error("Failed to check if index exists for knowledge base: {}", knowledgeBaseName, exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteKnowledgeBaseIndex(String knowledgeBaseName) {
        try {
            String indexName = generateIndexName(knowledgeBaseName);
            
            if (!indexExists(knowledgeBaseName)) {
                log.warn("Index does not exist for knowledge base: {}", knowledgeBaseName);
                return true;
            }

            DeleteIndexRequest request = DeleteIndexRequest.of(builder -> builder
                .index(indexName));

            elasticsearchClient.indices().delete(request);
            log.info("Successfully deleted index for knowledge base: {}", knowledgeBaseName);
            return true;

        } catch (Exception exception) {
            log.error("Failed to delete index for knowledge base: {}", knowledgeBaseName, exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String generateIndexName(String knowledgeBaseName) {
        // Get environment prefix, default to "default" if not set
        String environment = activeProfile != null ? activeProfile : "default";
        
        // Convert to lowercase and replace spaces/special characters with underscore
        // Elasticsearch index names must be lowercase
        String sanitizedName = knowledgeBaseName
            .toLowerCase()
            .replaceAll("[^a-z0-9_-]", "_")
            .replaceAll("_+", "_");
            
        // Format: knowledge_base_{environment}_{name}
        return "knowledge_base_" + environment + "_" + sanitizedName;
    }
}