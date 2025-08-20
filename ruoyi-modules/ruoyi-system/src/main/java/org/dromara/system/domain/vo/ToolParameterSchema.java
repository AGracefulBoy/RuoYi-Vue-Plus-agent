package org.dromara.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工具参数结构定义
 * 用于描述工具的输入参数
 *
 * @author ruoyi
 */
@Data
public class ToolParameterSchema implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数描述
     */
    private String desc;

    /**
     * 默认值
     */
    private String defaultValue;
}