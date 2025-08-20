package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysKnowledgeBase;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 知识库管理视图对象 sys_knowledge_base
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysKnowledgeBase.class)
public class SysKnowledgeBaseVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 知识库ID
     */
    @ExcelProperty(value = "知识库ID")
    private Long knowledgeBaseId;

    /**
     * 租户编号
     */
    private String tenantId;

    /**
     * 知识库名称
     */
    @ExcelProperty(value = "知识库名称")
    private String name;

    /**
     * 知识库描述
     */
    @ExcelProperty(value = "知识库描述")
    private String description;

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
     * 元数据列表（用于前端展示）
     */
    private List<KnowledgeBaseMetadata> metadataList;

    /**
     * 模型
     */
    @ExcelProperty(value = "模型")
    private Long model;

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
     * 状态（0正常 1停用）
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    private String delFlag;

    /**
     * 创建部门
     */
    private Long createDept;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建者名称
     */
    @ExcelProperty(value = "创建者名称")
    private String createByName;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新者名称
     */
    @ExcelProperty(value = "更新者名称")
    private String updateByName;

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
