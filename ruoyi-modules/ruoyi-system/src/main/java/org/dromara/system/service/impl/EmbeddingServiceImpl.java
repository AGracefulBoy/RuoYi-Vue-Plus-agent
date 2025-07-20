package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.dto.EmbeddingRequestDto;
import org.dromara.system.domain.dto.EmbeddingResponseDto;
import org.dromara.system.service.IEmbeddingService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

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

    @Qualifier("embeddingRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${embedding.api.url:http://www.hangtushuzhi.cn/embedding}")
    private String embeddingApiUrl;

    @Value("${embedding.api.enabled:true}")
    private Boolean embeddingEnabled;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Double> textToEmbedding(String text) {
        if (!embeddingEnabled) {
            throw new RuntimeException("Embedding service is disabled");
        }

        try {
            EmbeddingRequestDto request = EmbeddingRequestDto.of(text);
            EmbeddingResponseDto response = callEmbeddingApi(request);

            if (!response.isSuccess()) {
                throw new RuntimeException("Embedding API returned error: " + response.getMessage());
            }

            List<Double> embedding = response.getFirstEmbedding();
            if (embedding == null || embedding.isEmpty()) {
                throw new RuntimeException("No embedding vector returned from API");
            }

            log.debug("Successfully converted text to embedding vector with {} dimensions", embedding.size());
            return embedding;

        } catch (Exception exception) {
            log.error("Failed to convert text to embedding: {}", text, exception);
            throw new RuntimeException("Failed to convert text to embedding", exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<List<Double>> textsToEmbeddings(List<String> texts) {
        if (!embeddingEnabled) {
            throw new RuntimeException("Embedding service is disabled");
        }

        try {
            EmbeddingRequestDto request = EmbeddingRequestDto.of(texts);
            EmbeddingResponseDto response = callEmbeddingApi(request);

            if (!response.isSuccess()) {
                throw new RuntimeException("Embedding API returned error: " + response.getMessage());
            }

            List<List<Double>> embeddings = response.getData();
            if (embeddings == null || embeddings.isEmpty()) {
                throw new RuntimeException("No embedding vectors returned from API");
            }

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
     * {@inheritDoc}
     */
    @Override
    public float[] textToEmbeddingArray(String text) {
        List<Double> embedding = textToEmbedding(text);

        // Convert List<Double> to float array for Elasticsearch dense_vector
        float[] embeddingArray = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            embeddingArray[i] = embedding.get(i).floatValue();
        }

        return embeddingArray;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean isServiceAvailable() {
        if (!embeddingEnabled) {
            return false;
        }

        try {
            // Test with a simple text
            textToEmbedding("test");
            return true;
        } catch (Exception exception) {
            log.warn("Embedding service is not available: {}", exception.getMessage());
            return false;
        }
    }

    /**
     * Calls the external embedding API with HTTP request.
     *
     * @param request the embedding request DTO
     * @return the embedding response DTO
     * @throws RuntimeException if the API call fails
     */
    private EmbeddingResponseDto callEmbeddingApi(EmbeddingRequestDto request) {
        try {
            // Set up headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create HTTP entity
            HttpEntity<EmbeddingRequestDto> entity = new HttpEntity<>(request, headers);

            // Make the API call
            ResponseEntity<EmbeddingResponseDto> response = restTemplate.exchange(
                embeddingApiUrl,
                HttpMethod.POST,
                entity,
                EmbeddingResponseDto.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("HTTP request failed with status: " + response.getStatusCode());
            }

            EmbeddingResponseDto responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from embedding API");
            }

            return responseBody;

        } catch (Exception exception) {
            log.error("Failed to call embedding API at {}", embeddingApiUrl, exception);
            throw new RuntimeException("Failed to call embedding API", exception);
        }
    }
}
