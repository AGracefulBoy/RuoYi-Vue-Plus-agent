package org.dromara.system.domain.vo;


import cn.idev.excel.annotation.ExcelProperty;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.common.excel.annotation.ExcelDictFormat;
import org.dromara.common.excel.convert.ExcelDictConvert;
import org.dromara.system.domain.SysTool;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工具管理视图对象 sys_tool
 *
 * @author ruoyi
 */
@Data
@AutoMapper(target = SysTool.class)
public class SysToolVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    @ExcelProperty(value = "工具ID")
    private Long toolId;

    /**
     * 租户编号
     */
    @ExcelProperty(value = "租户编号")
    private String tenantId;

    /**
     * 工具名称
     */
    @ExcelProperty(value = "工具名称")
    private String toolName;

    /**
     * 工具描述
     */
    @ExcelProperty(value = "工具描述")
    private String toolDesc;

    /**
     * 函数名称
     */
    @ExcelProperty(value = "函数名称")
    private String functionName;

    /**
     * 工具类型（api、script、builtin等）
     */
    @ExcelProperty(value = "工具类型")
    private String toolType;

    /**
     * 是否流式处理（0否 1是）
     */
    @ExcelProperty(value = "是否流式处理", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    private String isStream;

    /**
     * 脚本代码
     */
    @ExcelProperty(value = "脚本代码")
    private String scriptCode;

    /**
     * 工具状态（0正常 1停用）
     */
    @ExcelProperty(value = "工具状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_normal_disable")
    private String toolStatus;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @ExcelProperty(value = "删除标志", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_show_hide")
    private String delFlag;

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
     * API配置信息（JSON格式）
     */
    private String apiConfig;

    /**
     * 参数模式定义（JSON Schema格式）
     */
    private String parameterSchema;

    /**
     * 结果模式定义（JSON Schema格式）
     */
    private String resultSchema;

    /**
     * 认证配置（JSON格式）
     */
    private String authConfig;

    /**
     * 执行超时时间（秒）
     */
    @ExcelProperty(value = "超时时间(秒)")
    private Integer timeoutSeconds;

    /**
     * 重试次数
     */
    @ExcelProperty(value = "重试次数")
    private Integer retryTimes;

    /**
     * 速率限制（次/分钟，0表示不限制）
     */
    @ExcelProperty(value = "速率限制")
    private Integer rateLimit;

}
