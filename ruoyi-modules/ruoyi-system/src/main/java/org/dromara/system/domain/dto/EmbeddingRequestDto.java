package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding request DTO for text vectorization.
 *
 * @author ruoyi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequestDto {

    /**
     * List of texts to be converted to embeddings.
     */
    private List<String> text;

    /**
     * Create request with single text.
     *
     * @param singleText the text to be embedded
     * @return embedding request DTO
     */
    public static EmbeddingRequestDto of(String singleText) {
        return new EmbeddingRequestDto(List.of(singleText));
    }

    /**
     * Create request with multiple texts.
     *
     * @param texts the texts to be embedded
     * @return embedding request DTO
     */
    public static EmbeddingRequestDto of(List<String> texts) {
        return new EmbeddingRequestDto(texts);
    }
}