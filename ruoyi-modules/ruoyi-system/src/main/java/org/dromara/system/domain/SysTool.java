package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/**
 * 工具管理对象 sys_tool
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tool")
public class SysTool extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
    @TableId(value = "tool_id")
    private Long toolId;

    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 工具描述
     */
    private String toolDesc;

    /**
     * 函数名称
     */
    private String functionName;

    /**
     * 工具类型（api、script、builtin等）
     */
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
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
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

} 