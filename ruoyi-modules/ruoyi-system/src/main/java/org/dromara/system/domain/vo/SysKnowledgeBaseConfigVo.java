package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysKnowledgeBaseConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库默认配置视图对象 sys_knowledge_base_config
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysKnowledgeBaseConfig.class)
public class SysKnowledgeBaseConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long knowledgeBaseConfigId;

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
    @ExcelProperty(value = "模型")
    private String model;

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

    /**
     * 创建部门
     */
    @ExcelProperty(value = "创建部门")
    private Long createDept;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private Long createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private Long updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

}
