package org.dromara.system.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.domain.instruction.ToolCallInstruction;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ReAct执行辅助类
 * 负责处理ReAct模式的推理循环和工具调用解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReActExecutionHelper {

    /**
     * 从AI响应中解析工具调用指令
     *
     * @param response       AI响应文本
     * @param availableTools 可用工具列表
     * @return 工具调用指令，如果没有找到则返回null
     */
    public ToolCallInstruction parseToolCallFromResponse(String response, List<ToolDto> availableTools) {
        if (!StringUtils.hasText(response) || CollectionUtils.isEmpty(availableTools)) {
            return null;
        }

        // 查找工具调用模式：提取Action:和Action Input:之间的内容作为工具名
        Pattern actionPattern = Pattern.compile(
            "Action:\\s*(.+?)(?=\\s*Action\\s+Input:|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher actionMatcher = actionPattern.matcher(response);

        if (actionMatcher.find()) {
            String toolName = actionMatcher.group(1).trim();

            // 验证工具是否可用（忽略大小写）
            ToolDto matchedTool = availableTools.stream()
                .filter(tool -> toolName.equalsIgnoreCase(tool.getName()))
                .findFirst()
                .orElse(null);

            if (matchedTool != null) {
                // 查找工具参数：Action Input: parameters
                // 支持多行JSON格式，匹配到下一个Action:或Observation:或字符串结尾
                Pattern inputPattern = Pattern.compile(
                    "Action Input:\\s*(.+?)(?=(?:\\n(?:Action|Observation|Thought):|\\z))", 
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher inputMatcher = inputPattern.matcher(response);

                String parameters = inputMatcher.find() ? inputMatcher.group(1).trim() : "{}";
                // 使用匹配到的工具的实际名称，确保大小写一致
                return new ToolCallInstruction(matchedTool.getName(), matchedTool.getId(), matchedTool.getType(), JsonRepairTool.repairJson(parameters));
            }
        }

        return null;
    }

    /**
     * 检查响应是否包含最终答案
     *
     * @param response AI响应文本
     * @return 是否包含最终答案
     */
    public boolean containsFinalAnswer(String response) {
        if (!StringUtils.hasText(response)) {
            return false;
        }

        String lowerResponse = response.toLowerCase();
        return lowerResponse.contains("final answer:") ||
            lowerResponse.contains("最终答案:") ||
            lowerResponse.contains("final answer：") ||
            lowerResponse.contains("最终答案：") ||
            lowerResponse.contains("answer:") ||
            lowerResponse.contains("答案:");
    }

    /**
     * 提取最终答案
     *
     * @param response AI响应文本
     * @return 提取的最终答案
     */
    public String extractFinalAnswer(String response) {
        if (!StringUtils.hasText(response)) {
            return response;
        }

        // 定义可能的最终答案标记
        String[] markers = {
            "Final Answer:", "final answer:", "FINAL ANSWER:",
            "最终答案:", "最终答案：",
            "Answer:", "answer:", "ANSWER:",
            "答案:", "答案："
        };

        for (String marker : markers) {
            int index = response.indexOf(marker);
            if (index != -1) {
                // 提取标记后的内容作为最终答案
                String answer = response.substring(index + marker.length()).trim();
                // 如果答案不为空，返回答案；否则继续尝试其他标记
                if (StringUtils.hasText(answer)) {
                    return answer;
                }
            }
        }

        // 如果没有找到标记，返回整个响应
        return response;
    }

    /**
     * 根据工具名称解析工具ID
     *
     * @param toolName 工具名称
     * @param ctx      流式上下文
     * @return 工具ID
     */
    public Long resolveToolId(String toolName, StreamingContext ctx) {
        // 从上下文中获取工具列表
        List<ToolDto> availableTools = ctx.getCurrentAvailableTools();
        if (availableTools == null || availableTools.isEmpty()) {
            throw new RuntimeException("无法找到可用工具列表");
        }

        // 查找工具
        return availableTools.stream()
            .filter(tool -> toolName.equals(tool.getName()))
            .map(ToolDto::getId)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("工具不存在: " + toolName));
    }

    /**
     * 添加到对话历史
     *
     * @param role    角色
     * @param content 内容
     * @param ctx     流式上下文
     */
    public void appendToConversationHistory(String role, String content, StreamingContext ctx) {
        // 对于ReAct格式的特殊处理
        if ("Observation".equals(role)) {
            // 观察结果使用特定格式
            ctx.getConversationHistory()
                .append(String.format("\nObservation: %s\n", content));
            log.debug("添加Observation到对话历史: {}", content);
        } else if ("Assistant".equals(role)) {
            // AI响应直接添加，不需要前缀
            ctx.getConversationHistory()
                .append(String.format("\n%s\n", content));
            log.debug("添加Assistant响应到对话历史: {}", content);
        } else {
            // 其他角色使用标准格式
            ctx.getConversationHistory()
                .append(String.format("\n%s: %s\n", role, content));
            log.debug("添加{}到对话历史: {}", role, content);
        }
    }

    /**
     * 获取对话历史
     *
     * @param ctx 流式上下文
     * @return 对话历史字符串
     */
    public String getConversationHistory(StreamingContext ctx) {
        return ctx.getConversationHistory().toString();
    }
}
