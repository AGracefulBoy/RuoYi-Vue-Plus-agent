package org.dromara.system.service.tool.executor;

import org.dromara.system.domain.vo.SysToolVo;

/**
 * 工具执行器接口
 * 定义工具执行的基本契约
 */
public interface IToolExecutor {
    
    /**
     * 执行工具
     *
     * @param tool       工具配置信息
     * @param parameters 执行参数（JSON格式）
     * @return 执行结果
     */
    ToolExecutionResult execute(SysToolVo tool, String parameters);
    
    /**
     * 判断是否支持该类型的工具
     *
     * @param toolType 工具类型
     * @return 是否支持
     */
    boolean supports(String toolType);
}