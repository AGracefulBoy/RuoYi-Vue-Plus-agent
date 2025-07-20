package org.dromara.system.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * Embedding response DTO for text vectorization results.
 *
 * @author ruoyi
 */
@Data
public class EmbeddingResponseDto {

    /**
     * List of embedding vectors.
     * Each vector corresponds to one input text.
     */
    private List<List<Double>> data;

    /**
     * Response status code (if provided by API).
     */
    private Integer code;

    /**
     * Response message (if provided by API).
     */
    private String message;

    /**
     * Check if the response is successful.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess() {
        return code == null || code == 200 || code == 0;
    }

    /**
     * Get the first embedding vector if exists.
     *
     * @return the first embedding vector, or null if not available
     */
    public List<Double> getFirstEmbedding() {
        return data != null && !data.isEmpty() ? data.get(0) : null;
    }
}
