package org.dromara.system.service.helper;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.ToolDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 提示词构建辅助类
 * 用于构建和管理各种提示词
 * 
 * @author system
 */
@Component
@RequiredArgsConstructor
public class PromptBuilderHelper {
    
    /**
     * 构建初始提示词
     */
    public String buildPrompt(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        // 获取系统提示模板
        String systemPrompt = agent.getPromptContent();
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = buildDefaultReActPrompt();
        }

        // 替换占位符
        String prompt = systemPrompt
            .replace("{{agent_personality}}", 
                StringUtils.hasText(agent.getAgentPersonality()) ? agent.getAgentPersonality() : "")
            .replace("{{tools}}", 
                CollectionUtils.isEmpty(availableTools) ? "无可用工具" : buildToolListDescription(availableTools))
            .replace("{{tool_list}}", 
                CollectionUtils.isEmpty(availableTools) ? "无可用工具" : buildToolListDescription(availableTools))
            .replace("{{query}}", userInput)
            .replace("{{input}}", userInput);

        return prompt;
    }
    
    /**
     * 构建指定步骤的提示词
     */
    public String buildPromptForStep(SysAgent agent, List<ToolDto> availableTools, 
                                    String userInput, int step, String conversationHistory) {
        if (step == 0) {
            // 第一步：使用原始提示词
            return buildPrompt(agent, availableTools, userInput);
        } else {
            // 后续步骤：包含完整的ReAct上下文
            StringBuilder prompt = new StringBuilder();
            
            // 添加系统提示和工具列表
            String systemPrompt = agent.getPromptContent();
            if (!StringUtils.hasText(systemPrompt)) {
                systemPrompt = buildDefaultReActPrompt();
            }
            
            // 替换占位符但保留工具列表和格式说明
            String basePrompt = systemPrompt
                .replace("{{agent_personality}}", 
                    StringUtils.hasText(agent.getAgentPersonality()) ? agent.getAgentPersonality() : "")
                .replace("{{tools}}", 
                    CollectionUtils.isEmpty(availableTools) ? "" : buildToolListDescription(availableTools))
                .replace("{{input}}", userInput);
            
            prompt.append(basePrompt);
            prompt.append(conversationHistory);
            
            return prompt.toString();
        }
    }
    
    /**
     * 构建默认的ReAct提示词模板
     */
    public String buildDefaultReActPrompt() {
        return """
            你是一个智能AI助手，善于进行推理并完成各种任务。

            请使用以下格式进行思考和操作：
            
            Thought: 分析当前状态和下一步需要做什么
            Action: 工具名称
            Action Input: 工具参数（JSON格式）
            Observation: 工具执行结果
            ... (必要时重复 Thought/Action/Action Input/Observation)
            Thought: 基于观察结果进行最终分析
            Final Answer: 最终答案

            可用工具：
            {{tool_list}}

            用户问题：{{query}}

            请开始推理：
            """;
    }
    
    /**
     * 构建工具列表描述
     */
    public String buildToolListDescription(List<ToolDto> availableTools) {
        if (CollectionUtils.isEmpty(availableTools)) {
            return "";
        }
        
        List<Map<String, Object>> toolList = new ArrayList<>();
        for (ToolDto tool : availableTools) {
            Map<String, Object> toolMap = new LinkedHashMap<>();
            
            if (StringUtils.hasText(tool.getDesc())) {
                toolMap.put("desc", tool.getDesc());
            }
            
            toolMap.put("name", tool.getName());
            
            if (!CollectionUtils.isEmpty(tool.getParameters())) {
                List<Map<String, Object>> paramList = new ArrayList<>();
                for (ToolDto.Parameter param : tool.getParameters()) {
                    Map<String, Object> paramMap = new LinkedHashMap<>();
                    
                    if (StringUtils.hasText(param.getDesc())) {
                        paramMap.put("desc", param.getDesc());
                    }
                    
                    paramMap.put("name", param.getName());
                    
                    if (param.getRequired() != null) {
                        paramMap.put("required", param.getRequired());
                    }
                    
                    if (StringUtils.hasText(param.getType())) {
                        paramMap.put("type", param.getType());
                    }
                    
                    paramList.add(paramMap);
                }
                toolMap.put("parameters", paramList);
            }
            
            toolList.add(toolMap);
        }
        
        return JSONUtil.toJsonPrettyStr(toolList);
    }
    
    /**
     * 构建增强回复的提示词
     */
    public String buildEnhancePrompt(String finalAnswer, String thoughtProcess) {
        StringBuilder enhancePrompt = new StringBuilder();
        enhancePrompt.append("请基于以下推理过程和初步答案，提供一个更优化、更完善的回复：\n\n");
        
        if (StringUtils.hasText(thoughtProcess) && thoughtProcess.length() > 0) {
            enhancePrompt.append("推理过程：\n");
            enhancePrompt.append(thoughtProcess);
            enhancePrompt.append("\n\n");
        }
        
        enhancePrompt.append("初步答案：\n");
        enhancePrompt.append(finalAnswer);
        enhancePrompt.append("\n\n请提供一个更加详细、准确和专业的回复。");
        
        return enhancePrompt.toString();
    }
    
    /**
     * 构建自由对话模式的提示词
     */
    public String buildFreeChatPrompt(SysAgent agent, String userInput) {
        // 获取系统提示
        String systemPrompt = agent.getPromptContent();
        
        // 如果没有系统提示，使用智能体的人格设定
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = agent.getAgentPersonality();
        }
        
        // 如果都没有，使用默认的
        if (!StringUtils.hasText(systemPrompt)) {
            systemPrompt = "你是一个专业的AI助手，请根据用户的问题提供帮助。";
        }
        
        // 构建完整提示词
        return systemPrompt + "\n\n用户：" + userInput + "\n\n助手：";
    }
}