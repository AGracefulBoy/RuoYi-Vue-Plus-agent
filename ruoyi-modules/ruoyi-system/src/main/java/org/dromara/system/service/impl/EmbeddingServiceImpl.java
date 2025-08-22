package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IEmbeddingModelService;
import org.dromara.common.llm.model.protocol.req.IEmbeddingRequest;
import org.dromara.system.domain.dto.EmbeddingRequestDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.IEmbeddingService;
import org.dromara.system.service.ISysModelConfigService;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Embedding service implementation for text vectorization.
 * Handles HTTP requests to external embedding API.
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class EmbeddingServiceImpl implements IEmbeddingService {

    @Autowired
    private ISysModelConfigService modelConfigService;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<Float>> textsToEmbeddings(List<String> texts, Long embeddingModel) {

        try {
            EmbeddingRequestDto request = EmbeddingRequestDto.of(texts);

            SysModelConfigVo modelConfig = modelConfigService.queryById(embeddingModel);
            List<float[]> floats = callEmbeddingApi(request, modelConfig);

            // Convert List<float[]> to List<List<Double>>
            List<List<Float>> embeddings = floats.stream()
                .map(floatArray -> {
                    List<Float> doubleList = new ArrayList<>();
                    for (float f : floatArray) {
                        doubleList.add((Float) f);
                    }
                    return doubleList;
                })
                .collect(Collectors.toList());

            if (embeddings.size() != texts.size()) {
                log.warn("Input texts count ({}) doesn't match output embeddings count ({})",
                    texts.size(), embeddings.size());
            }

            log.debug("Successfully converted {} texts to embedding vectors", texts.size());
            return embeddings;

        } catch (Exception exception) {
            log.error("Failed to convert texts to embeddings: {} texts", texts.size(), exception);
            throw new RuntimeException("Failed to convert texts to embeddings", exception);
        }
    }


    /**
     * Calls the external embedding API with HTTP request.
     *
     * @param request the embedding request DTO
     * @return the embedding response DTO
     * @throws RuntimeException if the API call fails
     */
    private List<float[]> callEmbeddingApi(EmbeddingRequestDto request, SysModelConfigVo modelConfig) {
        try {
            IEmbeddingModelService embeddingService = AiService.getEmbeddingService(modelConfig.getModelProvider());

            IEmbeddingRequest text = IEmbeddingRequest.builder()
                .baseUrl(modelConfig.getBaseUrl())
                .apiKey(modelConfig.getApiKey())
                .model(modelConfig.getModelCode())
                .text(request.getText())
                .build();
            EmbeddingResponse call = embeddingService.call(text);

            return call.getResults().stream().map(Embedding::getOutput).toList();

        } catch (Exception exception) {
            log.error("Failed to call embedding ", exception);
            throw new RuntimeException("Failed to call embedding", exception);
        }
    }
}
