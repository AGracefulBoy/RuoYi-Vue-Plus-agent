package org.dromara.system.service.impl;

import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IEmbeddingModelService;
import org.dromara.common.llm.model.protocol.req.IEmbeddingRequest;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysKnowledgeBaseDocument;
import org.dromara.system.domain.SysKnowledgeBaseDocumentChunk;
import org.dromara.system.domain.dto.HitDocumentDTO;
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.vo.SysKnowledgeBaseEsDocumentVo;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentChunkMapper;
import org.dromara.system.mapper.SysKnowledgeBaseDocumentMapper;
import org.dromara.system.service.*;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private ISysModelConfigService modelConfigService;

    private final ElasticsearchClient elasticsearchClient;
    private final IElasticsearchIndexService elasticsearchIndexService;
    private final SysKnowledgeBaseDocumentMapper knowledgeBaseDocumentMapper;
    private final SysKnowledgeBaseDocumentChunkMapper knowledgeBaseDocumentChunkMapper;
    private final ISysKnowledgeBaseService knowledgeBaseService;
    private final IReRankService reRankService;


    @Value("${embedding.api.connect-timeout:30000}")
    private Integer connectTimeout;

    @Value("${embedding.api.read-timeout:30000}")
    private Integer readTimeout;

    private final OkHttpClient httpClient;

    private static final List<String> INCLUDED_FIELDS = Arrays.asList("content", "fileName", "documentId", "createTime", "metadata");

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

            // Check if index exists before querying
            try {
                ExistsRequest existsRequest = ExistsRequest.of(builder -> builder.index(indexName));
                boolean indexExists = elasticsearchClient.indices().exists(existsRequest).value();
                if (!indexExists) {
                    log.warn("Index does not exist: {}", indexName);
                    return TableDataInfo.build();
                }
            } catch (Exception e) {
                log.error("Failed to check index existence: {}", indexName, e);
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
                        .field("createTime.keyword")
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
                        .field("documentId.keyword")
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
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteChunkByChunkId(String chunkId) {
        try {
            log.info("Deleting ES chunk by chunkId: {}", chunkId);

            // First, find the chunk record to get document ID and knowledge base ID
            SysKnowledgeBaseDocumentChunk chunk = knowledgeBaseDocumentChunkMapper.selectById(Long.valueOf(chunkId));
            if (chunk == null) {
                log.warn("Chunk not found in database: chunkId={}", chunkId);
                return false;
            }

            Long documentId = chunk.getDocumentId();
            Long knowledgeBaseId = chunk.getKnowledgeBaseId();

            if (documentId == null || knowledgeBaseId == null) {
                log.warn("Invalid chunk data: chunkId={}, documentId={}, knowledgeBaseId={}",
                    chunkId, documentId, knowledgeBaseId);
                return false;
            }

            // Generate index name using knowledge base ID
            String indexName = elasticsearchIndexService.generateIndexName(knowledgeBaseId.toString());

            // Delete the specific chunk document from ES
            DeleteRequest deleteRequest = DeleteRequest.of(builder -> builder
                .index(indexName)
                .id(chunkId)
                .refresh(Refresh.True) // Refresh index after deletion
            );

            var deleteResponse = elasticsearchClient.delete(deleteRequest);
            boolean success = deleteResponse.result().jsonValue().equals("deleted");

            if (success) {
                log.info("Successfully deleted ES chunk: chunkId={}", chunkId);
            } else {
                log.warn("ES chunk not found or already deleted: chunkId={}", chunkId);
            }

            return success;

        } catch (Exception exception) {
            log.error("Failed to delete ES chunk: chunkId={}", chunkId, exception);
            return false;
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
        esDocument.setChunkTitle((String) source.get("chunkTitle"));
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

    @Autowired
    private IEmbeddingService iEmbeddingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse<Map> embeddingSearch(String indexName, Integer size, String question,
                                               Map<String, Object> metadata, SysModelConfigVo modelConfig) {
        if (metadata != null) {
            log.info("Embedding search with metadata: {}", JSONUtil.toJsonStr(metadata));
        }

        try {
            List<List<Float>> lists = iEmbeddingService.textsToEmbeddings(List.of(question), modelConfig.getModelId());
            // 构建查询条件
            KnnSearch.Builder knnBuilder = new KnnSearch.Builder()
                .field("embedding")
                .k(size)
                .numCandidates(100)
                .queryVector(lists.get(0));

            // 添加元数据过滤
            if (metadata != null && !metadata.isEmpty()) {
                List<Query> filters = buildMetadataFilters(metadata);
                if (!filters.isEmpty()) {
                    knnBuilder.filter(filters);
                }
            }

            SearchRequest searchRequest = new SearchRequest.Builder()
                .index(indexName)
                .knn(knnBuilder.build())
                .size(size)
                .source(src -> src
                    .filter(f -> f
                        .includes(INCLUDED_FIELDS)
                    )
                )
                .build();

            log.debug("Embedding search DSL: {}", searchRequest.toString());
            return elasticsearchClient.search(searchRequest, Map.class);

        } catch (Exception e) {
            log.error("向量搜索发生异常", e);
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SearchResponse<Map> keywordSearch(String indexName, Integer size, String question,
                                             Map<String, Object> metadata) {
        try {
            // 构建多字段匹配查询
            Query multiMatchQuery = Query.of(q -> q
                .multiMatch(m -> m
                    .fields(Collections.singletonList("content"))
                    .type(TextQueryType.BestFields)
                    .query(question)
                )
            );

            // 构建最终查询
            Query finalQuery;
            if (metadata != null && !metadata.isEmpty()) {
                List<Query> filters = buildMetadataFilters(metadata);
                finalQuery = Query.of(q -> q
                    .bool(b -> b
                        .must(multiMatchQuery)
                        .filter(filters)
                    )
                );
            } else {
                finalQuery = multiMatchQuery;
            }

            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.of(s -> s
                .index(indexName)
                .query(finalQuery)
                .from(0)
                .size(size)
            );

            return elasticsearchClient.search(searchRequest, Map.class);
        } catch (Exception e) {
            log.error("关键词搜索发生异常: indexName={}, question={}", indexName, question, e);
            throw new ServiceException("关键词搜索失败");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<HitSourceDTO> hybridSearch(Long knowledgeBaseId, String question, String metadata, Boolean isKnowledge) {
        SysKnowledgeBaseVo knowledgeBase = knowledgeBaseService.queryById(knowledgeBaseId);

        if (knowledgeBase == null) {
            log.warn("知识库不存在: {}", knowledgeBaseId);
            return new ArrayList<>();
        }

        String indexName = elasticsearchIndexService.generateIndexName(knowledgeBase.getKnowledgeBaseId().toString());
        Integer size = knowledgeBase.getTopK();
        Map<String, Object> paramObject = null;

        if (isKnowledge && metadata != null) {
            paramObject = JSONUtil.toBean(metadata, Map.class);
        }

        SysModelConfigVo modelConfig = modelConfigService.queryById(knowledgeBase.getEmbeddingModel());
        // 执行向量搜索和关键词搜索
        SearchResponse<Map> vectorResponse = embeddingSearch(indexName, size * 2, question, paramObject, modelConfig);
        SearchResponse<Map> keywordResponse = keywordSearch(indexName, size * 2, question, paramObject);

        if (vectorResponse == null || keywordResponse == null) {
            return new ArrayList<>();
        }

        // 归一化处理
        List<HitSourceDTO> vectorResults = processSearchResponse(vectorResponse);
        List<HitSourceDTO> keywordResults = processSearchResponse(keywordResponse);

        if (vectorResults.isEmpty() && keywordResults.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取最大和最小分数用于归一化
        double vectorMaxScore = vectorResults.isEmpty() ? 0.0 : vectorResults.get(0).getScore();
        double vectorMinScore = vectorResults.isEmpty() ? 0.0 : vectorResults.get(vectorResults.size() - 1).getScore();

        double keywordMaxScore = keywordResults.isEmpty() ? 0.0 : keywordResults.get(0).getScore();
        double keywordMinScore = keywordResults.isEmpty() ? 0.0 : keywordResults.get(keywordResults.size() - 1).getScore();

        Map<String, HitSourceDTO> mergedResults = new HashMap<>();

        // 归一化处理,融合结果
        for (HitSourceDTO vectorResult : vectorResults) {
            Double score = vectorResult.getScore();
            vectorResult.setNormalizedScore(normalize(score, vectorMinScore, vectorMaxScore) * knowledgeBase.getVectorWeight());
            mergedResults.put(vectorResult.getHitDocument().getDocumentId() + "_" + vectorResult.getId(), vectorResult);
        }

        for (HitSourceDTO keywordResult : keywordResults) {
            Double score = keywordResult.getScore();
            keywordResult.setNormalizedScore(normalize(score, keywordMinScore, keywordMaxScore) * (1 - knowledgeBase.getVectorWeight()));
            String key = keywordResult.getHitDocument().getDocumentId() + "_" + keywordResult.getId();

            if (mergedResults.containsKey(key)) {
                // 如果已存在，比较分数，保留分数较高的结果
                HitSourceDTO existing = mergedResults.get(key);
                double existingScore = existing.getNormalizedScore();
                double currentScore = keywordResult.getNormalizedScore();

                if (currentScore > existingScore) {
                    mergedResults.put(key, keywordResult);
                }
            } else {
                mergedResults.put(key, keywordResult);
            }
        }

        List<HitSourceDTO> sortedResults = mergedResults.values().stream()
            // 根据 normalizedScore 降序排序
            .sorted(Comparator.comparingDouble(HitSourceDTO::getNormalizedScore).reversed())
            // 收集为 List
            .collect(Collectors.toList());

        // 调用reRank 方法进行重排序
        List<String> reRankList = new ArrayList<>();
        for (HitSourceDTO hitSourceDTO : sortedResults) {
            reRankList.add(hitSourceDTO.getHitDocument().getContent());
        }

        List<Double> reRankScoreList = reRankService.reRank(question, reRankList, knowledgeBase);

        for (int i = 0; i < sortedResults.size(); i++) {
            sortedResults.get(i).setReRandScore(reRankScoreList.get(i));
        }

        // 按照 reRandScore 降序排序
        sortedResults.sort((a, b) -> Double.compare(b.getReRandScore(), a.getReRandScore()));

        // 返回前 size 个结果
        return sortedResults.subList(0, Math.min(size, sortedResults.size()));
    }

    /**
     * 构建元数据过滤条件
     */
    private List<Query> buildMetadataFilters(Map<String, Object> metadata) {
        List<Query> filters = new ArrayList<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            String fieldName = "metadata." + entry.getKey() + ".keyword";
            Object value = entry.getValue();

            // 处理值为 List 的情况（OR 关系）
            if (value instanceof List) {
                List<Query> shouldQueries = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    shouldQueries.add(Query.of(q -> q
                        .term(t -> t
                            .field(fieldName)
                            .value(FieldValue.of(item.toString()))
                        )
                    ));
                }
                // 将同一个 key 下的多个 value 用 should (OR) 组合
                filters.add(Query.of(q -> q
                    .bool(b -> b
                        .should(shouldQueries)
                        .minimumShouldMatch("1")
                    )
                ));
            }
            // 处理单个值的情况
            else {
                filters.add(Query.of(q -> q
                    .term(t -> t
                        .field(fieldName)
                        .value(FieldValue.of(value.toString()))
                    )
                ));
            }
        }

        return filters;
    }

    /**
     * 准备向量化请求体
     */
    private String prepareRequestBody(List<String> texts) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("text", texts);
        return JSONUtil.toJsonStr(requestMap);
    }


    /**
     * 处理搜索响应，将搜索结果转换为HitSourceDTO列表
     */
    private List<HitSourceDTO> processSearchResponse(SearchResponse<Map> searchResponse) {
        List<HitSourceDTO> results = new ArrayList<>();
        for (Hit<Map> hit : searchResponse.hits().hits()) {
            HitSourceDTO hitSourceDTO = new HitSourceDTO();
            hitSourceDTO.setId(hit.id());
            hitSourceDTO.setScore(hit.score() != null ? hit.score().doubleValue() : 0.0);

            HitDocumentDTO hitDocumentDTO = new HitDocumentDTO();
            if (hit.source() != null) {
                Map<String, Object> source = hit.source();

                hitDocumentDTO.setFileName(source.getOrDefault("fileName", "").toString());
                Object createTimeObj = source.get("createTime");
                if (createTimeObj instanceof Long) {
                    hitDocumentDTO.setCreateTime((Long) createTimeObj);
                }
                hitDocumentDTO.setChunkTitle(source.getOrDefault("chunkTitle", "").toString());

                Object documentIdObj = source.get("documentId");
                if (documentIdObj instanceof String) {
                    hitDocumentDTO.setDocumentId(Long.valueOf(documentIdObj.toString()));
                } else if (documentIdObj instanceof Long) {
                    hitDocumentDTO.setDocumentId((Long) documentIdObj);
                } else if (documentIdObj instanceof Integer) {
                    hitDocumentDTO.setDocumentId(((Integer) documentIdObj).longValue());
                }

                hitDocumentDTO.setContent(source.getOrDefault("content", "").toString());

                Object metadataObj = source.get("metadata");
                if (metadataObj != null) {
                    hitDocumentDTO.setMetadata(metadataObj.toString());
                } else {
                    hitDocumentDTO.setMetadata("");
                }
            }

            hitSourceDTO.setHitDocument(hitDocumentDTO);
            results.add(hitSourceDTO);
        }
        return results;
    }

    /**
     * 归一化分数
     */
    private double normalize(Double score, double minScore, double maxScore) {
        if (maxScore == minScore) {
            return 0.5;
        }
        return (score - minScore) / (maxScore - minScore);
    }
}
