package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.core.validate.AddGroup;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.system.domain.SysTool;

import java.util.List;

/**
 * 工具管理业务对象 sys_tool
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SysTool.class, reverseConvertGenerate = false)
public class SysToolBo extends BaseEntity {

    /**
     * 工具ID
     */
    @NotNull(message = "工具ID不能为空", groups = { EditGroup.class })
    private Long toolId;

    /**
     * 工具名称
     */
    @NotBlank(message = "工具名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(min = 0, max = 100, message = "工具名称长度不能超过{max}个字符")
    private String toolName;

    /**
     * 工具描述
     */
    @Size(min = 0, max = 500, message = "工具描述长度不能超过{max}个字符")
    private String toolDesc;

    /**
     * 函数名称
     */
    @NotBlank(message = "函数名称不能为空", groups = { EditGroup.class })
    @Size(min = 0, max = 200, message = "函数名称长度不能超过{max}个字符")
    private String functionName;

    /**
     * 工具类型（api、script、builtin等）
     */
    @NotBlank(message = "工具类型不能为空", groups = { EditGroup.class })
    @Size(min = 0, max = 50, message = "工具类型长度不能超过{max}个字符")
    private String toolType;

    /**
     * 是否流式处理（0否 1是）
     */
    private String isStream;

    /**
     * 脚本代码
     */
    private String scriptCode;

    /**
     * 工具状态（0正常 1停用）
     */
    private String toolStatus;

    /**
     * 备注
     */
    @Size(min = 0, max = 500, message = "备注长度不能超过{max}个字符")
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
    private Integer timeoutSeconds;

    /**
     * 重试次数
     */
    private Integer retryTimes;

    /**
     * 速率限制（次/分钟，0表示不限制）
     */
    private Integer rateLimit;

    /**
     * Python版本
     */
    @Size(min = 0, max = 20, message = "Python版本长度不能超过{max}个字符")
    private String pythonVersion;

    /**
     * 虚拟环境名称
     */
    private String venvName;

    /**
     * 虚拟环境路径
     */
    private String venvPath;

    /**
     * 环境状态 (pending/creating/ready/error)
     */
    private String venvStatus;

}
