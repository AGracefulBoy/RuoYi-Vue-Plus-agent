package org.dromara.system.service.split.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.split.DocumentChunk;
import org.dromara.system.service.split.DocumentSplitter;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TXT document splitter implementation.
 * Splits text documents into chunks based on character size limits.
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class TxtDocumentSplitter implements DocumentSplitter {

    private static final String TXT_TYPE = "txt";

    @Override
    public List<DocumentChunk> split(String documentUrl, Integer chunkSize, Integer overlapSize) {
        try {
            String content = readTextFromUrl(documentUrl);
            return createChunks(content, chunkSize, overlapSize);
        } catch (IOException exception) {
            log.error("Failed to read TXT document from URL: {}", documentUrl, exception);
            throw new RuntimeException("Failed to read TXT document", exception);
        }
    }

    @Override
    public boolean supports(String documentType) {
        return TXT_TYPE.equalsIgnoreCase(documentType);
    }

    /**
     * Read text content from URL.
     *
     * @param documentUrl the URL to read from
     * @return the text content
     * @throws IOException if reading fails
     */
    private String readTextFromUrl(String documentUrl) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new URL(documentUrl).openStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Create chunks from text content.
     *
     * @param content     the text content
     * @param chunkSize   the maximum size of each chunk
     * @param overlapSize the overlap between chunks
     * @return list of document chunks
     */
    private List<DocumentChunk> createChunks(String content, Integer chunkSize, Integer overlapSize) {
        List<DocumentChunk> chunks = new ArrayList<>();
        
        if (StrUtil.isBlank(content)) {
            return chunks;
        }

        int contentLength = content.length();
        int chunkIndex = 0;
        int startPos = 0;

        while (startPos < contentLength) {
            int endPos = Math.min(startPos + chunkSize, contentLength);
            String chunkContent = content.substring(startPos, endPos);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("startPosition", startPos);
            metadata.put("endPosition", endPos);

            DocumentChunk chunk = DocumentChunk.builder()
                .content(chunkContent.trim())
                .chunkIndex(chunkIndex++)
                .metadata(metadata)
                .characterCount(chunkContent.trim().length())
                .sourceLocation("txt")
                .build();

            chunks.add(chunk);

            // If we've reached the end of content, stop
            if (endPos >= contentLength) {
                break;
            }

            // Calculate next start position, ensuring we always move forward
            startPos = endPos - overlapSize;
            // Ensure we always advance by at least 1 character
            if (startPos <= endPos - chunkSize) {
                startPos = endPos - chunkSize + 1;
            }
        }

        log.debug("Created {} chunks from text document", chunks.size());
        return chunks;
    }
}