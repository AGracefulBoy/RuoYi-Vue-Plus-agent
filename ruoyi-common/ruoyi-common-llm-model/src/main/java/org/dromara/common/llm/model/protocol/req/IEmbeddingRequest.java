package org.dromara.common.llm.model.protocol.req;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Builder
public class IEmbeddingRequest {
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

    private List<String> text;
}
