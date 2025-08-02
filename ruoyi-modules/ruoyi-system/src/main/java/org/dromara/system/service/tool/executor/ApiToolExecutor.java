package org.dromara.system.service.tool.executor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.service.tool.ToolConfigParser;
import org.dromara.system.service.tool.ToolParameterValidator;
import org.dromara.system.service.tool.config.ApiConfig;
import org.dromara.system.service.tool.config.AuthConfig;
import org.springframework.stereotype.Component;

/**
 * API工具执行器
 * 负责执行API类型的工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiToolExecutor implements IToolExecutor {
    
    private final ToolConfigParser configParser;
    private final ToolParameterValidator parameterValidator;
    
    @Override
    public ToolExecutionResult execute(SysToolVo tool, String parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 验证参数
            if (tool.getParameterSchema() != null) {
                ToolParameterValidator.ValidationResult validation = 
                    parameterValidator.validate(parameters, tool.getParameterSchema());
                if (!validation.isValid()) {
                    return ToolExecutionResult.failure("参数验证失败: " + validation.getErrorMessage());
                }
            }
            
            // 2. 解析配置
            ApiConfig apiConfig = configParser.parseApiConfig(tool.getApiConfig());
            if (apiConfig == null) {
                return ToolExecutionResult.failure("API配置解析失败");
            }
            
            AuthConfig authConfig = configParser.parseAuthConfig(tool.getAuthConfig());
            
            // 3. TODO: 实际的API调用逻辑将在TaskAgentServiceImpl中实现
            // 这里只是预留接口，避免过度设计
            
            return ToolExecutionResult.success("API工具执行器已就绪，等待集成到TaskAgentService");
            
        } catch (Exception e) {
            log.error("执行API工具失败: {}", tool.getToolName(), e);
            return ToolExecutionResult.failure("执行失败: " + e.getMessage());
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("工具 {} 执行耗时: {}ms", tool.getToolName(), executionTime);
        }
    }
    
    @Override
    public boolean supports(String toolType) {
        return "api".equalsIgnoreCase(toolType);
    }
}