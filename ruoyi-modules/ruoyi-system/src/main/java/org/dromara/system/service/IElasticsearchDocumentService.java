package org.dromara.system.service;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.vo.SysKnowledgeBaseEsDocumentVo;
import org.dromara.system.domain.vo.SysKnowledgeBaseVo;
import org.dromara.system.domain.vo.SysModelConfigVo;

import java.util.List;
import java.util.Map;

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

    /**
     * 向量搜索
     * 使用KNN查询进行向量相似度搜索
     *
     * @param indexName      索引名称（知识库ID）
     * @param size           返回结果数量
     * @param question       搜索问题
     * @param metadata       元数据过滤条件
     * @return 搜索结果
     */
    SearchResponse<Map> embeddingSearch(String indexName, Integer size, String question,
                                        Map<String, Object> metadata, SysModelConfigVo modelConfig);

    /**
     * 关键词搜索
     * 使用match查询进行文本匹配搜索
     *
     * @param indexName      索引名称（知识库ID）
     * @param size           返回结果数量
     * @param question       搜索问题
     * @param metadata       元数据过滤条件
     * @return 搜索结果
     */
    SearchResponse<Map> keywordSearch(String indexName, Integer size, String question,
                                     Map<String, Object> metadata);

    /**
     * 混合搜索
     * 结合向量搜索和关键词搜索的结果
     *
     * @param knowledgeBaseId 知识库ID
     * @param question        搜索问题
     * @param metadata        元数据过滤条件（JSON字符串）
     * @param isKnowledge     是否为知识库搜索
     * @return 搜索结果列表
     */
    List<HitSourceDTO> hybridSearch(Long knowledgeBaseId, String question, String metadata, Boolean isKnowledge);
}
