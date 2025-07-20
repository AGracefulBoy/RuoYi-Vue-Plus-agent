package org.dromara.system.service.split.impl;

import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.split.DocumentChunk;
import org.dromara.system.service.split.DocumentSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PDF document splitter implementation.
 * Currently not implemented - placeholder for future extension.
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class PdfDocumentSplitter implements DocumentSplitter {

    private static final String PDF_TYPE = "pdf";

    @Override
    public List<DocumentChunk> split(String documentUrl, Integer chunkSize, Integer overlapSize) {
        log.warn("PDF document splitting is not yet implemented for URL: {}", documentUrl);
        throw new UnsupportedOperationException("PDF document splitting is not yet implemented");
    }

    @Override
    public boolean supports(String documentType) {
        return PDF_TYPE.equalsIgnoreCase(documentType);
    }
}