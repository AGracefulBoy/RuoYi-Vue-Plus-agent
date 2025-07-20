package org.dromara.system.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.IDocumentSplitService;
import org.dromara.system.service.split.DocumentChunk;
import org.dromara.system.service.split.DocumentSplitter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Document split service implementation.
 * Manages document splitting by delegating to appropriate splitters.
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSplitServiceImpl implements IDocumentSplitService {

    private final List<DocumentSplitter> documentSplitters;

    @Override
    public List<DocumentChunk> splitDocument(String documentUrl, String documentType,
                                           Integer chunkSize, Integer overlapSize) {
        if (StrUtil.isBlank(documentUrl)) {
            throw new IllegalArgumentException("Document URL cannot be blank");
        }

        if (StrUtil.isBlank(documentType)) {
            throw new IllegalArgumentException("Document type cannot be blank");
        }

        if (chunkSize == null || chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive");
        }

        if (overlapSize == null || overlapSize < 0) {
            throw new IllegalArgumentException("Overlap size cannot be negative");
        }

        DocumentSplitter splitter = findSplitterForType(documentType);

        log.info("Splitting document: URL={}, type={}, chunkSize={}, overlapSize={}",
                documentUrl, documentType, chunkSize, overlapSize);

        List<DocumentChunk> chunks = splitter.split(documentUrl, chunkSize, overlapSize);

        log.info("Document split completed: {} chunks created", chunks.size());

        return chunks;
    }

    @Override
    public boolean isDocumentTypeSupported(String documentType) {
        if (StrUtil.isBlank(documentType)) {
            return false;
        }

        return documentSplitters.stream()
            .anyMatch(splitter -> splitter.supports(documentType));
    }

    @Override
    public List<String> getSupportedDocumentTypes() {
        return List.of("excel", "docx", "pdf");
    }

    /**
     * Find appropriate splitter for the given document type.
     *
     * @param documentType the document type
     * @return the matching document splitter
     * @throws IllegalArgumentException if no splitter supports the document type
     */
    private DocumentSplitter findSplitterForType(String documentType) {
        Optional<DocumentSplitter> splitter = documentSplitters.stream()
            .filter(s -> s.supports(documentType))
            .findFirst();

        return splitter.orElseThrow(() ->
            new IllegalArgumentException("Unsupported document type: " + documentType));
    }
}
