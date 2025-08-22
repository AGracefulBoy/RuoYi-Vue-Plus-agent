package org.dromara.system.service;

import java.util.List;

/**
 * Embedding service interface for text vectorization.
 * Provides methods to convert text content into vector embeddings.
 *
 * @author ruoyi
 */
public interface IEmbeddingService {

    /**
     * Convert multiple texts to embedding vectors.
     *
     * @param texts the list of texts to be converted
     * @return the list of embedding vectors, each vector corresponds to one input text
     * @throws RuntimeException if the embedding conversion fails
     */
    List<List<Float>> textsToEmbeddings(List<String> texts,Long embeddingModel);
}
