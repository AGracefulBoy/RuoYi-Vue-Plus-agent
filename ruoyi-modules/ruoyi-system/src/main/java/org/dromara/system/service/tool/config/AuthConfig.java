package org.dromara.system.service.tool.config;

import lombok.Data;

/**
 * API认证配置类
 * 用于解析sys_tool表中的auth_config字段
 */
@Data
public class AuthConfig {
    
    /**
     * 认证类型（bearer、api_key、basic等）
     */
    private String type;
    
    /**
     * 认证令牌（用于bearer token）
     */
    private String token;
    
    /**
     * API密钥（用于api_key认证）
     */
    private String apiKey;
    
    /**
     * API密钥的header名称（默认为X-API-Key）
     */
    private String apiKeyHeader = "X-API-Key";
    
    /**
     * 用户名（用于basic认证）
     */
    private String username;
    
    /**
     * 密码（用于basic认证）
     */
    private String password;
}