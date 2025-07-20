package org.dromara.system.service.split.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.FastExcel;
import cn.idev.excel.context.AnalysisContext;
import cn.idev.excel.event.AnalysisEventListener;
import cn.idev.excel.metadata.data.ReadCellData;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.split.DocumentChunk;
import org.dromara.system.service.split.DocumentSplitter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * Excel document splitter implementation.
 * Splits Excel documents by reading all cells and creating chunks based on size limits.
 *
 * @author ruoyi
 */
@Slf4j
@Component
public class ExcelDocumentSplitter implements DocumentSplitter {

    private static final String EXCEL_TYPE_XLSX = "excel";
    private static final String EXCEL_TYPE_XLS = "xls";

    @Override
    public List<DocumentChunk> split(String documentUrl, Integer chunkSize, Integer overlapSize) {
        List<DocumentChunk> chunks = new ArrayList<>();

        try (InputStream inputStream = new URL(documentUrl).openStream()) {
            ExcelContentCollector collector = new ExcelContentCollector();
            FastExcel.read(inputStream)
                .registerReadListener(collector)
                .doReadAll();

            return createChunksFromContent(collector.getSheetContents(), chunkSize, overlapSize);

        } catch (IOException exception) {
            log.error("Failed to read Excel document from URL: {}", documentUrl, exception);
            throw new RuntimeException("Failed to read Excel document", exception);
        }
    }

    @Override
    public boolean supports(String documentType) {
        return EXCEL_TYPE_XLSX.equalsIgnoreCase(documentType) ||
               EXCEL_TYPE_XLS.equalsIgnoreCase(documentType);
    }

    /**
     * Create chunks from collected Excel content.
     * Each row becomes a separate chunk for Excel documents.
     *
     * @param sheetContents map of sheet names to their rows
     * @param chunkSize     ignored for Excel documents
     * @param overlapSize   ignored for Excel documents
     * @return list of document chunks
     */
    private List<DocumentChunk> createChunksFromContent(Map<String, List<String>> sheetContents,
                                                       Integer chunkSize, Integer overlapSize) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int globalChunkIndex = 0;

        for (Map.Entry<String, List<String>> entry : sheetContents.entrySet()) {
            String sheetName = entry.getKey();
            List<String> rows = entry.getValue();

            if (CollUtil.isEmpty(rows)) {
                continue;
            }

            for (String rowContent : rows) {
                if (StrUtil.isBlank(rowContent)) {
                    continue;
                }

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("sheetName", sheetName);
                metadata.put("rowIndex", globalChunkIndex);

                DocumentChunk chunk = DocumentChunk.builder()
                    .content(rowContent)
                    .chunkIndex(globalChunkIndex++)
                    .metadata(metadata)
                    .characterCount(rowContent.length())
                    .sourceLocation(sheetName)
                    .build();

                chunks.add(chunk);
            }
        }

        return chunks;
    }


    /**
     * Excel content collector that reads all cells and organizes content by sheet.
     * Each row is stored as a separate item in the list.
     */
    private static class ExcelContentCollector extends AnalysisEventListener<Map<Integer, String>> {

        private final Map<String, List<String>> sheetContents = new LinkedHashMap<>();
        private String currentSheetName;

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            if (CollUtil.isEmpty(data)) {
                return;
            }

            String sheetName = getCurrentSheetName(context);
            List<String> sheetRows = sheetContents.computeIfAbsent(sheetName, k -> new ArrayList<>());

            // Join row data with tab separation
            StringJoiner rowJoiner = new StringJoiner("\t");
            data.values().stream()
                .filter(StrUtil::isNotBlank)
                .forEach(rowJoiner::add);

            String rowContent = rowJoiner.toString();
            if (StrUtil.isNotBlank(rowContent)) {
                sheetRows.add(rowContent);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            int totalRows = sheetContents.values().stream()
                .mapToInt(List::size)
                .sum();
            log.debug("Excel analysis completed for {} sheets with {} total rows", 
                sheetContents.size(), totalRows);
        }

        @Override
        public void invokeHead(Map<Integer, ReadCellData<?>> headMap, AnalysisContext context) {
            currentSheetName = getCurrentSheetName(context);
        }

        /**
         * Get current sheet name from context.
         *
         * @param context analysis context
         * @return sheet name or default name
         */
        private String getCurrentSheetName(AnalysisContext context) {
            String sheetName = context.readSheetHolder().getSheetName();
            return StrUtil.isNotBlank(sheetName) ? sheetName : "Sheet" + context.readSheetHolder().getSheetNo();
        }

        public Map<String, List<String>> getSheetContents() {
            return sheetContents;
        }
    }
}
