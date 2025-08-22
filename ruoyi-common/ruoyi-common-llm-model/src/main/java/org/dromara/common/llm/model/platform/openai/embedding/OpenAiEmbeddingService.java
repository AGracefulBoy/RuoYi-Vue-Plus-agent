package org.dromara.common.llm.model.platform.openai.embedding;


import org.dromara.common.llm.model.platform.IEmbeddingModelService;
import org.dromara.common.llm.model.protocol.req.IEmbeddingRequest;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

@Service
public class OpenAiEmbeddingService implements IEmbeddingModelService {

    public EmbeddingResponse call(IEmbeddingRequest iEmbeddingRequest) {
        OpenAiApi openAiApi = OpenAiApi.builder()
            .apiKey(iEmbeddingRequest.getApiKey())
            .baseUrl(iEmbeddingRequest.getBaseUrl())
            .build();

        OpenAiEmbeddingModel built = new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED);

        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
            .model(iEmbeddingRequest.getModel())
            .dimensions(1024)
            .build();

        return built.call(new EmbeddingRequest(iEmbeddingRequest.getText(), options));

    }
}
