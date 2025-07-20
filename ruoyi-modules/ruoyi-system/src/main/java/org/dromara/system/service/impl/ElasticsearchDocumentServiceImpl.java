package org.dromara.system.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.vo.SysKnowledgeBaseEsDocumentVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.IElasticsearchDocumentService;
import org.dromara.system.service.IElasticsearchIndexService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch document management service implementation.
 * Handles querying and deleting documents in Elasticsearch indices.
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class ElasticsearchDocumentServiceImpl implements IElasticsearchDocumentService {

    private final ElasticsearchClient elasticsearchClient;
    private final IElasticsearchIndexService elasticsearchIndexService;
    private final SysKnowledgeBaseDocumentMapper knowledgeBaseDocumentMapper;

    /**
     * {@inheritDoc}
     */
    @Override
    public TableDataInfo<SysKnowledgeBaseEsDocumentVo> queryDocumentsByDocumentId(Long documentId, PageQuery pageQuery) {
        try {
            log.info("Querying ES documents for documentId: {}", documentId);

            // First, get the knowledge base ID from the document
            String indexName = getIndexNameByDocumentId(documentId);
            if (indexName == null) {
                log.warn("Cannot find knowledge base for documentId: {}", documentId);
                return TableDataInfo.build();
            }

            // Calculate pagination parameters
            int from = (int) ((pageQuery.getPageNum() - 1) * pageQuery.getPageSize());
            int size = (int) pageQuery.getPageSize();

            // Build search request with source filtering to exclude embedding field
            SearchRequest searchRequest = SearchRequest.of(builder -> builder
                .index(indexName)
                .query(query -> query
                    .term(term -> term
                        .field("documentId")
                        .value(documentId.toString())
                    )
                )
                .source(source -> source
                    .filter(filter -> filter
                        .excludes("embedding") // Exclude embedding field
                    )
                )
                .from(from)
                .size(size)
                .sort(sort -> sort
                    .field(field -> field
                        .field("createTime")
                        .order(SortOrder.Desc)
                    )
                )
            );

            SearchResponse<Map> searchResponse = elasticsearchClient.search(searchRequest, Map.class);

            // Convert hits to result list
            List<SysKnowledgeBaseEsDocumentVo> documents = new ArrayList<>();
            for (Hit<Map> hit : searchResponse.hits().hits()) {
                if (hit.source() != null) {
                    SysKnowledgeBaseEsDocumentVo esDocument = convertToEsDocumentVo(hit.source(), hit.id());
                    documents.add(esDocument);
                }
            }

            // Get total count
            long total = searchResponse.hits().total() != null ? searchResponse.hits().total().value() : 0;

            log.info("Found {} ES documents for documentId: {}, showing {} results", total, documentId, documents.size());

            return new TableDataInfo<>(documents, total);

        } catch (Exception exception) {
            log.error("Failed to query ES documents for documentId: {}", documentId, exception);
            return TableDataInfo.build();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteDocumentsByDocumentId(Long documentId) {
        try {
            log.info("Deleting ES documents for documentId: {}", documentId);

            String indexName = getIndexNameByDocumentId(documentId);
            if (indexName == null) {
                log.warn("Cannot find knowledge base for documentId: {}", documentId);
                return false;
            }

            // Delete all documents with the specified documentId
            DeleteByQueryRequest deleteRequest = DeleteByQueryRequest.of(builder -> builder
                .index(indexName)
                .query(query -> query
                    .term(term -> term
                        .field("documentId")
                        .value(documentId.toString())
                    )
                )
                .refresh(Boolean.TRUE) // Refresh index after deletion
            );

            var deleteResponse = elasticsearchClient.deleteByQuery(deleteRequest);
            long deletedCount = deleteResponse.deleted() != null ? deleteResponse.deleted() : 0;

            log.info("Successfully deleted {} ES documents for documentId: {}", deletedCount, documentId);
            return true;

        } catch (Exception exception) {
            log.error("Failed to delete ES documents for documentId: {}", documentId, exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteChunkById(Long documentId, Long chunkId) {
        try {
            log.info("Deleting ES chunk: documentId={}, chunkId={}", documentId, chunkId);

            String indexName = getIndexNameByDocumentId(documentId);
            if (indexName == null) {
                log.warn("Cannot find knowledge base for documentId: {}", documentId);
                return false;
            }

            // Delete the specific chunk document
            DeleteRequest deleteRequest = DeleteRequest.of(builder -> builder
                .index(indexName)
                .id(chunkId.toString())
                .refresh(Refresh.True) // Refresh index after deletion
            );

            var deleteResponse = elasticsearchClient.delete(deleteRequest);
            boolean success = deleteResponse.result().jsonValue().equals("deleted");

            if (success) {
                log.info("Successfully deleted ES chunk: documentId={}, chunkId={}", documentId, chunkId);
            } else {
                log.warn("ES chunk not found or already deleted: documentId={}, chunkId={}", documentId, chunkId);
            }

            return success;

        } catch (Exception exception) {
            log.error("Failed to delete ES chunk: documentId={}, chunkId={}", documentId, chunkId, exception);
            return false;
        }
    }

    /**
     * Gets the Elasticsearch index name for a given document ID.
     * Looks up the knowledge base ID from the document and generates the index name.
     *
     * @param documentId the document ID
     * @return the ES index name, or null if document not found
     */
    private String getIndexNameByDocumentId(Long documentId) {
        try {
            // Query the knowledge base document to get knowledge base ID
            LambdaQueryWrapper<SysKnowledgeBaseDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysKnowledgeBaseDocument::getDocumentId, documentId)
                       .select(SysKnowledgeBaseDocument::getKnowledgeBaseId);

            SysKnowledgeBaseDocument document = knowledgeBaseDocumentMapper.selectOne(queryWrapper);
            if (document == null) {
                log.warn("Document not found: documentId={}", documentId);
                return null;
            }

            Long knowledgeBaseId = document.getKnowledgeBaseId();
            if (knowledgeBaseId == null) {
                log.warn("Knowledge base ID is null for documentId: {}", documentId);
                return null;
            }

            // Generate index name using knowledge base ID as the name
            return elasticsearchIndexService.generateIndexName(knowledgeBaseId.toString());

        } catch (Exception exception) {
            log.error("Failed to get index name for documentId: {}", documentId, exception);
            return null;
        }
    }

    /**
     * Converts ES hit source map to SysKnowledgeBaseEsDocumentVo.
     *
     * @param source the ES hit source map
     * @param hitId  the ES document ID
     * @return converted ES document VO
     */
    @SuppressWarnings("unchecked")
    private SysKnowledgeBaseEsDocumentVo convertToEsDocumentVo(Map<String, Object> source, String hitId) {
        SysKnowledgeBaseEsDocumentVo esDocument = new SysKnowledgeBaseEsDocumentVo();

        esDocument.setId(hitId);
        esDocument.setDocumentId((String) source.get("documentId"));
        esDocument.setContent((String) source.get("content"));
        esDocument.setFileName((String) source.get("fileName"));
        esDocument.setCreateTime((String) source.get("createTime"));

        // Handle metadata - ensure it's properly cast to Map<String, Object>
        Object metadataObj = source.get("metadata");
        if (metadataObj instanceof Map) {
            esDocument.setMetadata((Map<String, Object>) metadataObj);
        } else {
            esDocument.setMetadata(new HashMap<>());
        }

        return esDocument;
    }
}
