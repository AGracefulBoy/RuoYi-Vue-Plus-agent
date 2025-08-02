package org.dromara.system.service.tool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.service.tool.config.ApiConfig;
import org.dromara.system.service.tool.config.AuthConfig;
import org.springframework.stereotype.Component;

/**
 * 工具配置解析器
 * 负责解析工具相关的JSON配置
 */
@Slf4j
@Component
public class ToolConfigParser {
    
    /**
     * 解析API配置
     *
     * @param apiConfigJson API配置的JSON字符串
     * @return API配置对象，如果解析失败返回null
     */
    public ApiConfig parseApiConfig(String apiConfigJson) {
        if (StrUtil.isBlank(apiConfigJson)) {
            return null;
        }
        
        try {
            return JSONUtil.toBean(apiConfigJson, ApiConfig.class);
        } catch (Exception e) {
            log.error("解析API配置失败: {}", apiConfigJson, e);
            return null;
        }
    }
    
    /**
     * 解析认证配置
     *
     * @param authConfigJson 认证配置的JSON字符串
     * @return 认证配置对象，如果解析失败返回null
     */
    public AuthConfig parseAuthConfig(String authConfigJson) {
        if (StrUtil.isBlank(authConfigJson)) {
            return null;
        }
        
        try {
            return JSONUtil.toBean(authConfigJson, AuthConfig.class);
        } catch (Exception e) {
            log.error("解析认证配置失败: {}", authConfigJson, e);
            return null;
        }
    }
}