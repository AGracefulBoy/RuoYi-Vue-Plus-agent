package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * Elasticsearch knowledge base document view object.
 * Represents a document chunk stored in Elasticsearch without embedding field.
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
public class SysKnowledgeBaseEsDocumentVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Elasticsearch document ID (chunk ID) - can be used for deletion
     */
    @ExcelProperty(value = "ES文档块ID")
    private String id;

    /**
     * Document ID that this chunk belongs to
     */
    @ExcelProperty(value = "文档ID")
    private String documentId;

    /**
     * Content of the document chunk
     */
    @ExcelProperty(value = "文档内容")
    private String content;

    /**
     * File name of the original document
     */
    @ExcelProperty(value = "文件名")
    private String fileName;

    /**
     * Metadata associated with the document chunk
     */
    private Map<String, Object> metadata;

    /**
     * Creation time of the document in Elasticsearch
     */
    @ExcelProperty(value = "创建时间")
    private String createTime;
}