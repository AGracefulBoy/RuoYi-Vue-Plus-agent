package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具简要信息视图对象
 * 用于智能体详情中的工具列表展示
 *
 * @author 系统管理员
 */
@Data
public class SysToolSimpleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工具ID
     */
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
     * 工具状态（0正常 1停用）
     */
    private String toolStatus;
}