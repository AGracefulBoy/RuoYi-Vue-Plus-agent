package org.dromara.system.domain.dto;

import lombok.Data;

/**
 * 知识库搜索结果命中项数据传输对象
 *
 * @author ruoyi
 */
@Data
public class HitSourceDTO {
    
    /**
     * 文档片段ID
     */
    private String id;
    
    /**
     * 相关性分数
     */
    private Double score;
    
    /**
     * 归一化分数
     */
    private Double normalizedScore;
    
    /**
     * 重排序分数
     */
    private Double reRandScore;
    
    /**
     * 命中文档详情
     */
    private HitDocumentDTO hitDocument;
}