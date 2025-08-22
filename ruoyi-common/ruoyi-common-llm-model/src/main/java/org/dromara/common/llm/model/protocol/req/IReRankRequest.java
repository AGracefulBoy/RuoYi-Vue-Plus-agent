package org.dromara.common.llm.model.protocol.req;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IReRankRequest {
    /**
     * 对话模型
     */
    private String baseUrl;

    /**
     * 在遇到这些词时，API 将停止生成更多的 token。
     */
    private String apiKey;
    /**
     * 对话模型
     */
    private String model;

    private Integer topN;
    /**
     * 待rerank的文本
     */
    private String query;
    /**
     * 待重新排序的文档列表
     */
    private List<String> documents;
}
