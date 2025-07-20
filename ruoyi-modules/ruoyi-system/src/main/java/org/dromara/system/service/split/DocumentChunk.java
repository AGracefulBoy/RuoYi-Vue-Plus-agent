package org.dromara.system.service.split;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Document chunk data structure representing a portion of a document.
 *
 * @author ruoyi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * Content of this chunk
     */
    private String content;

    /**
     * Chunk sequence number within the document
     */
    private Integer chunkIndex;

    /**
     * Additional metadata for this chunk
     */
    private Map<String, Object> metadata;

    /**
     * Character count of this chunk
     */
    private Integer characterCount;

    /**
     * Source location information (e.g., sheet name, page number)
     */
    private String sourceLocation;
}