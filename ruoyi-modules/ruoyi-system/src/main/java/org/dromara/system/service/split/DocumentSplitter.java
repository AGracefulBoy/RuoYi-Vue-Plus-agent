package org.dromara.system.service.split;

import java.util.List;

/**
 * Document splitter interface for various document types.
 * Provides a common contract for splitting documents into chunks.
 *
 * @author ruoyi
 */
public interface DocumentSplitter {

    /**
     * Split document from URL into chunks.
     *
     * @param documentUrl   the document URL to split
     * @param chunkSize     the maximum size of each chunk in characters
     * @param overlapSize   the overlap size between chunks in characters
     * @return list of document chunks
     */
    List<DocumentChunk> split(String documentUrl, Integer chunkSize, Integer overlapSize);

    /**
     * Check if this splitter supports the given document type.
     *
     * @param documentType the document type (e.g., "xlsx", "docx", "pdf")
     * @return true if this splitter supports the document type, false otherwise
     */
    boolean supports(String documentType);
}