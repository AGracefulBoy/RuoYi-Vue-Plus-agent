package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识库默认配置视图对象 sys_knowledge_base_config
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
public class SysKnowledgeBaseConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 向量检索返回条数
     */
    @ExcelProperty(value = "向量检索返回条数")
    private Integer topK;

    /**
     * 向量检索权重
     */
    @ExcelProperty(value = "向量检索权重")
    private Float vectorWeight;

    /**
     * 元数据
     */
    @ExcelProperty(value = "元数据")
    private String metadata;

    /**
     * 模型
     */
    private Long model;

    /**
     * 视觉模型
     */
    private Long imageModel;

    /**
     * 向量模型
     */
    private Long embeddingModel;

    /**
     * 重排序模型
     */
    private Long rerankModel;

    /**
     * 块大小
     */
    @ExcelProperty(value = "块大小")
    private Integer blockSize;

    /**
     * 重叠字数
     */
    @ExcelProperty(value = "重叠字数")
    private Integer overlapSize;

    /**
     * 切片提示词
     */
    @ExcelProperty(value = "切片提示词")
    private String slicePrompt;

    /**
     * 图片识别提示词
     */
    @ExcelProperty(value = "图片识别提示词")
    private String imagePrompt;
}
