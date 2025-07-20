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
     * Convert a single text to embedding vector.
     *
     * @param text the text to be converted
     * @return the embedding vector as a list of doubles
     * @throws RuntimeException if the embedding conversion fails
     */
    List<Double> textToEmbedding(String text);

    /**
     * Convert multiple texts to embedding vectors.
     *
     * @param texts the list of texts to be converted
     * @return the list of embedding vectors, each vector corresponds to one input text
     * @throws RuntimeException if the embedding conversion fails
     */
    List<List<Double>> textsToEmbeddings(List<String> texts);

    /**
     * Convert a single text to embedding vector as float array.
     * This method is useful for direct storage in Elasticsearch dense_vector field.
     *
     * @param text the text to be converted
     * @return the embedding vector as a float array
     * @throws RuntimeException if the embedding conversion fails
     */
    float[] textToEmbeddingArray(String text);

    /**
     * Check if the embedding service is available.
     *
     * @return true if the service is available, false otherwise
     */
    Boolean isServiceAvailable();
}