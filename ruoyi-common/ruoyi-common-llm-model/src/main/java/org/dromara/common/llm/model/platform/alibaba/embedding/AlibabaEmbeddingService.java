package org.dromara.common.llm.model.platform.alibaba.embedding;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.dromara.common.llm.model.platform.IEmbeddingModelService;
import org.dromara.common.llm.model.protocol.req.IEmbeddingRequest;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

@Service
public class AlibabaEmbeddingService implements IEmbeddingModelService {
    public EmbeddingResponse call(IEmbeddingRequest iEmbeddingRequest) {
        DashScopeApi dashScopeApi = DashScopeApi.builder()
            .apiKey(iEmbeddingRequest.getApiKey())
            .baseUrl(iEmbeddingRequest.getBaseUrl())
            .build();

        DashScopeEmbeddingModel built = new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED);

        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
            .withModel(iEmbeddingRequest.getModel())
            .withDimensions(1024)
            .build();

        return built.call(new EmbeddingRequest(iEmbeddingRequest.getText(), options));

    }
}
