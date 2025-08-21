package org.dromara.system.domain.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysModelConfig;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 模型配置视图对象 sys_model_config
 *
 * @author 系统管理员
 */
@Data
@ExcelIgnoreUnannotated
@AutoMapper(target = SysModelConfig.class)
public class SysModelConfigVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模型ID
     */
    @ExcelProperty(value = "模型ID")
    private Long modelId;

    /**
     * 租户编号
     */
    @ExcelProperty(value = "租户编号")
    private String tenantId;

    /**
     * 模型编码
     */
    @ExcelProperty(value = "模型编码")
    private String modelCode;

    /**
     * 模型厂商（openai、baidu、alibaba、tencent等）
     */
    @ExcelProperty(value = "模型厂商")
    private String modelProvider;

    /**
     * 模型类型（支持多种类型：chat聊天、embedding嵌入、image图像等）
     */
    @ExcelProperty(value = "模型类型")
    private List<String> modelType;

    /**
     * 模型API地址
     */
    @ExcelProperty(value = "模型API地址")
    private String baseUrl;

    /**
     * API密钥
     */
    @ExcelProperty(value = "API密钥")
    private String apiKey;

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

    /**
     * 是否默认模型（0否 1是）
     * 非数据库字段，用于返回时标识
     */
    private Integer isDefault;

}
