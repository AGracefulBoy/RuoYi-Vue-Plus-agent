package org.dromara.system.domain.bo;

import lombok.Data;

import java.util.Map;

/**
 * 工具调试请求对象
 *
 * @author ruoyi
 */
@Data
public class ToolDebugRequestBo {

    /**
     * 工具ID
     */
    private Long toolId;

    /**
     * 函数参数（Map对象）
     */
    private Map<String, Object> params;

    /**
     * 是否使用流式响应
     */
    private Boolean stream = false;
}