package org.dromara.system.service.tool.config;

import lombok.Data;

import java.util.Map;

/**
 * API工具配置类
 * 用于解析sys_tool表中的api_config字段
 */
@Data
public class ApiConfig {
    
    /**
     * API请求URL
     */
    private String url;
    
    /**
     * HTTP请求方法（GET、POST、PUT、DELETE等）
     */
    private String method = "GET";
    
    /**
     * 请求头信息
     */
    private Map<String, String> headers;
    
    /**
     * 请求超时时间（秒），如果不设置则使用tool表中的timeoutSeconds
     */
    private Integer timeout;
}