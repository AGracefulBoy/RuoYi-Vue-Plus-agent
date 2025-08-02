package org.dromara.system.service.tool.executor;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具执行结果
 */
@Data
public class ToolExecutionResult {
    
    /**
     * 执行是否成功
     */
    private boolean success;
    
    /**
     * 执行结果数据
     */
    private Object data;
    
    /**
     * 错误信息（执行失败时）
     */
    private String errorMessage;
    
    /**
     * 执行耗时（毫秒）
     */
    private long executionTime;
    
    /**
     * 附加信息
     */
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * 创建成功结果
     */
    public static ToolExecutionResult success(Object data) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }
    
    /**
     * 创建失败结果
     */
    public static ToolExecutionResult failure(String errorMessage) {
        ToolExecutionResult result = new ToolExecutionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
    
    /**
     * 添加元数据
     */
    public ToolExecutionResult addMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }
}