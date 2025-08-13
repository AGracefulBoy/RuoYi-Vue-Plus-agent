package org.dromara.system.domain.dto;

import lombok.Data;

/**
 * 搜索命中文档数据传输对象
 */
@Data
public class HitDocumentDTO {

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 向量化内容
     */
    private String chunkTitle;

    /**
     * 文档ID
     */
    private Long documentId;


    /**
     * 内容
     */
    private String content;

    /**
     * 元数据
     */
    private String metadata;
}
