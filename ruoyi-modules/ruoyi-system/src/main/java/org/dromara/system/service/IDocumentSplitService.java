package org.dromara.system.service;

import org.dromara.system.service.split.DocumentChunk;

import java.util.List;

/**
 * Document split service interface.
 * Provides unified document splitting functionality for various document types.
 *
 * @author ruoyi
 */
public interface IDocumentSplitService {

    /**
     * Split document from URL into chunks.
     *
     * @param documentUrl   the document URL to split
     * @param documentType  the document type (e.g., "xlsx", "docx", "pdf")
     * @param chunkSize     the maximum size of each chunk in characters
     * @param overlapSize   the overlap size between chunks in characters
     * @return list of document chunks
     * @throws IllegalArgumentException if document type is not supported
     */
    List<DocumentChunk> splitDocument(String documentUrl, String documentType, 
                                     Integer chunkSize, Integer overlapSize);

    /**
     * Check if the given document type is supported.
     *
     * @param documentType the document type to check
     * @return true if supported, false otherwise
     */
    boolean isDocumentTypeSupported(String documentType);

    /**
     * Get all supported document types.
     *
     * @return list of supported document types
     */
    List<String> getSupportedDocumentTypes();
}