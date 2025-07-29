package org.dromara.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.ISysModelConfigService;
import org.dromara.system.service.TaskAgentService;
import org.dromara.system.service.ChatContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务智能体服务实现类
 *
 * <p>ReAct循环处理流程：</p>
 * <pre>
 * 1. 🤔 思考 (Thought) - AI分析问题和当前状态
 * 2. 🔧 行动 (Action) - 选择并执行工具调用
 * 3. 👀 观察 (Observation) - 获取工具执行结果
 * 4. 🔄 重复上述步骤直到获得最终答案
 * 5. ✨ 完成 (Final Answer) - 提供最终结果
 * </pre>
 *
 * <p>使用示例：</p>
 * <pre>
 * // 示例1: 基础流式调用（支持ReAct循环）
 * Flux&lt;String&gt; stream = taskAgentService.executeReActStream(agent, tools, "查询今天的天气");
 * stream.subscribe(
 *     text -> System.out.print(text), // 实时显示推理过程
 *     error -> log.error("调用失败", error),
 *     () -> log.info("推理完成")
 * );
 *
 * // 示例2: 带完整响应的调用
 * StreamResult result = taskAgentService.executeReActStreamWithFullResponse(agent, tools,
 *     "帮我分析这个数据并生成报告");
 *
 * // 实时显示推理过程
 * result.getStream().subscribe(text -> System.out.print(text));
 *
 * // 获取完整的推理过程和结果
 * result.getFullResponseFuture().thenAccept(fullResponse -> {
 *     log.info("完整推理过程和结果: {}", fullResponse);
 *     // 保存完整推理过程到数据库等操作
 * });
 *
 * // 工具调用示例（AI会自动解析并调用）
 * // AI响应: "Thought: 我需要查询天气信息
 * //          Action: weather_query
 * //          Action Input: {\"city\": \"北京\", \"date\": \"today\"}"
 * // 系统会自动：
 * // 1. 解析出工具名称: weather_query
 * // 2. 解析出参数: {"city": "北京", "date": "today"}
 * // 3. 执行工具调用
 * // 4. 将结果返回给AI继续推理
 * </pre>
 *
 * <p>支持的AI响应格式：</p>
 * <pre>
 * Thought: 分析和思考
 * Action: tool_name
 * Action Input: {"param1": "value1", "param2": "value2"}
 *
 * 或直接提供最终答案：
 * Final Answer: 这是最终答案
 * </pre>
 *
 * @author assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAgentServiceImpl implements TaskAgentService {

    @Autowired
    private AiService aiService;

    @Autowired
    private ISysModelConfigService modelConfigService;

    @Autowired
    private ChatContextService chatContextService;

    @Override
    public Flux<StreamMessageResponseDto> executeReActStream(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        SaTokenContext context = SaHolder.getContext();

        return Flux.<StreamMessageResponseDto>create(sink -> {
            try {
                SaTokenContextMockUtil.setMockContext(() -> {
                    SaManager.setSaTokenContext(context);
                });
                // 清理线程本地变量
                clearThreadLocals();
                // 执行流式推理
                performStreamingReasoningChain(agent, availableTools, userInput, sink);

            } catch (Exception e) {
                log.error("流式ReAct执行失败", e);
                // 发送错误消息
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    e.getMessage(),
                    currentChatId.get(),
                    generateMessageId(),
                    getAndIncrementMessageIndex()
                ));
                sink.complete();
            }
        }).doFinally(signal -> clearThreadLocals()); // 确保清理线程本地变量
    }

    @Override
    public StreamResult executeReActStreamWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        CompletableFuture<String> fullResponseFuture = new CompletableFuture<>();

        Flux<String> stream = Flux.create(sink -> {
            try {
                // 执行流式推理，并在完成时设置完整响应
                performStreamingReasoningChainWithFullResponse(agent, availableTools, userInput, sink, fullResponseFuture);

            } catch (Exception e) {
                log.error("流式ReAct执行失败", e);
                fullResponseFuture.completeExceptionally(e);
                sink.error(e);
            }
        });

        return new StreamResult(stream, fullResponseFuture);
    }


    /**
     * 执行流式推理链
     */
    private void performStreamingReasoningChain(SysAgent agent, List<ToolDto> availableTools,
                                                String userInput, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink) {
        try {
            // 1. 获取并验证模型配置
            ModelConfigContext modelContext = getAndValidateModelConfigsForStreamMessage(agent, sink);
            if (modelContext == null) return;

            // 2. 创建或获取会话
            SysAgentChat chat = createOrGetChat(agent.getAgentId(), userInput);
            Long chatId = chat.getChatId();
            String chatIdStr = String.valueOf(chatId);

            // 设置会话信息和用户消息ID
            String userMsgId = generateMessageId();
            setCurrentChatInfo(chatIdStr, userMsgId);

            // 3. 保存用户消息
            saveUserMessage(chatId, userInput);

            // 5. 启动ReAct循环
            startReActLoopWithChat(agent, availableTools, userInput, modelContext, sink, 0, chatId);

        } catch (Exception e) {
            log.error("流式推理执行失败", e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "流式推理执行失败: " + e.getMessage(),
                currentChatId.get(),
                generateMessageId(),
                getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 启动ReAct循环（带会话管理）
     */
    private void startReActLoopWithChat(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                        ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                        int currentStep, Long chatId) {
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep, chatId);
    }

    /**
     * 启动ReAct循环（核心实现）
     */
    private void startReActLoop(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                int currentStep, Long chatId) {

        // 设置最大循环次数防止无限循环
        final int MAX_STEPS = 20;
        if (currentStep >= MAX_STEPS) {
            sink.next(createStreamMessage("\n\n⚠️ 已达到最大推理步数限制，停止执行\n", "answer", false));
            // 使用handleFinalStepCompleteForMessage确保增强处理逻辑能执行
            handleFinalStepCompleteForMessage("已达到最大推理步数限制", modelContext, sink, response -> {
            });
            return;
        }

        try {
            // 构建当前步骤的提示词
            String cotPrompt = buildPromptForStep(agent, availableTools, userInput, currentStep);

            // 执行流式AI调用
            executeStreamingAICallForMessage(cotPrompt, modelContext, sink, fullResponse -> {
                log.info("第{}步获取到完整响应: {}", currentStep + 1, fullResponse);

                // 解析响应，判断是否需要执行工具调用
                processReActResponse(agent, availableTools, userInput, fullResponse,
                    modelContext, sink, currentStep, chatId);
            }, false); // 标记为中间步骤

        } catch (Exception e) {
            log.error("ReAct循环执行失败，步骤: {}", currentStep + 1, e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "ReAct循环执行失败: " + e.getMessage(),
                currentChatId.get(),
                generateMessageId(),
                getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 处理ReAct响应
     */
    private void processReActResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                      String aiResponse, ModelConfigContext modelContext,
                                      reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, int currentStep, Long chatId) {

        try {
            // 解析AI响应，查找工具调用指令
            ToolCallInstruction toolCall = parseToolCallFromResponse(aiResponse, availableTools);

            if (toolCall != null) {
                // 保存思考步骤（包含工具调用决策）
                saveThoughtStep(chatId, aiResponse, "action", currentStep);

                // 需要执行工具调用
                sink.next(createStreamMessage(String.format("\n🔧 **执行工具:** %s\n", toolCall.getToolName()), "action", false));
                sink.next(createStreamMessage(String.format("📝 **参数:** %s\n", toolCall.getParameters()), "action", false));

                // 执行工具调用
                String toolResult = executeToolCall(toolCall);

                sink.next(createStreamMessage(String.format("✅ **工具结果:** %s\n", toolResult), "observation", false));

                // 保存工具执行结果
                saveToolResult(chatId, toolCall, toolResult, currentStep);

                // 将工具执行结果添加到对话历史中，继续下一轮
                appendToolResultAndContinue(agent, availableTools, userInput, aiResponse,
                    toolCall, toolResult, modelContext, sink, currentStep, chatId);

            } else {
                // 没有工具调用，检查是否包含最终答案
                if (containsFinalAnswer(aiResponse)) {
                    sink.next(createStreamMessage("\n✨ **推理完成**\n", "answer", false));
                    // 使用handleFinalStepCompleteForMessage确保增强处理逻辑能执行
                    String finalAnswer = extractFinalAnswer(aiResponse);
                    handleFinalStepCompleteForMessage(finalAnswer, modelContext, sink, response -> {
                    });
                } else {
                    // 保存思考步骤
                    saveThoughtStep(chatId, aiResponse, "thought", currentStep);

                    // 继续推理
                    continueReasoning(agent, availableTools, userInput, aiResponse,
                        modelContext, sink, currentStep, chatId);
                }
            }

        } catch (Exception e) {
            log.error("处理ReAct响应失败", e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "处理ReAct响应失败: " + e.getMessage(),
                currentChatId.get(),
                generateMessageId(),
                getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 构建指定步骤的提示词
     */
    private String buildPromptForStep(SysAgent agent, List<ToolDto> availableTools,
                                      String userInput, int step) {

        if (step == 0) {
            // 第一步：使用原始提示词
            return buildPrompt(agent, availableTools, userInput);
        } else {
            // 后续步骤：从历史记录中构建
            return getConversationHistory() + "\n\n请继续推理并决定下一步行动。";
        }
    }

    /**
     * 继续推理（没有工具调用的情况）
     */
    private void continueReasoning(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                   String previousResponse, ModelConfigContext modelContext,
                                   reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, int currentStep, Long chatId) {

        // 将之前的响应添加到历史记录
        appendToConversationHistory("Assistant", previousResponse);

        // 继续下一轮推理
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep + 1, chatId);
    }

    /**
     * 添加工具结果并继纭
     */
    private void appendToolResultAndContinue(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                             String aiResponse, ToolCallInstruction toolCall, String toolResult,
                                             ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                             int currentStep, Long chatId) {

        // 将AI响应和工具执行结果添加到历史记录
        appendToConversationHistory("Assistant", aiResponse);
        appendToConversationHistory("Tool", String.format("工具 %s 执行结果: %s",
            toolCall.getToolName(), toolResult));

        // 继续下一轮推理
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep + 1, chatId);
    }

    // 对话历史记录
    private final ThreadLocal<StringBuilder> conversationHistory = ThreadLocal.withInitial(StringBuilder::new);

    // 消息索引计数器
    private final ThreadLocal<Integer> messageIndexCounter = ThreadLocal.withInitial(() -> 0);

    // 当前会话ID
    private final ThreadLocal<String> currentChatId = ThreadLocal.withInitial(() -> null);

    // 用户消息ID（用于回复）
    private final ThreadLocal<String> userMessageId = ThreadLocal.withInitial(() -> null);

    /**
     * 添加到对话历史
     */
    private void appendToConversationHistory(String role, String content) {
        conversationHistory.get()
            .append(String.format("\n%s: %s\n", role, content));
    }

    /**
     * 获取对话历史
     */
    private String getConversationHistory() {
        return conversationHistory.get().toString();
    }


    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return String.valueOf(System.currentTimeMillis() + (int) (Math.random() * 1000));
    }

    /**
     * 获取并增加消息索引
     */
    private int getAndIncrementMessageIndex() {
        Integer current = messageIndexCounter.get();
        messageIndexCounter.set(current + 1);
        return current;
    }

    /**
     * 重置消息索引
     */
    private void resetMessageIndex() {
        messageIndexCounter.set(0);
    }

    /**
     * 设置当前会话信息
     */
    private void setCurrentChatInfo(String chatId, String userMsgId) {
        currentChatId.set(chatId);
        userMessageId.set(userMsgId);
    }

    /**
     * 清理线程本地变量
     */
    private void clearThreadLocals() {
        resetMessageIndex();
        currentChatId.remove();
        userMessageId.remove();
    }

    /**
     * 创建或获取会话
     */
    private SysAgentChat createOrGetChat(Long agentId, String userInput) {
        Long userId = LoginHelper.getUserId();
        // 为每个新的ReAct流创建新会话
        String title = userInput.length() > 50 ? userInput.substring(0, 50) + "..." : userInput;
        return chatContextService.createChat(agentId, userId, title);
    }

    /**
     * 保存用户消息
     */
    private void saveUserMessage(Long chatId, String userInput) {
        SysAgentChatMessage userMessage = new SysAgentChatMessage();
        userMessage.setChatId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(userInput);
        userMessage.setMessageType("text");
        userMessage.setStatus("completed");
        userMessage.setProcessingTime(0L);

        chatContextService.addMessage(userMessage);
    }

    /**
     * 保存思考步骤
     */
    private void saveThoughtStep(Long chatId, String content, String messageType, int stepIndex) {
        if (chatId == null) return;

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("assistant");
        message.setContent(content);
        message.setMessageType(messageType);
        message.setStatus("completed");

        // 创建思维链步骤信息
        List<SysAgentChatMessage.ThoughtStep> thoughtSteps = new ArrayList<>();
        SysAgentChatMessage.ThoughtStep step = new SysAgentChatMessage.ThoughtStep();
        step.setStepType(messageType);
        step.setContent(content);
        step.setStepIndex(stepIndex);
        step.setSuccess(true);
        thoughtSteps.add(step);
        message.setThoughtSteps(thoughtSteps);

        chatContextService.addMessage(message);
    }

    /**
     * 保存工具执行结果
     */
    private void saveToolResult(Long chatId, ToolCallInstruction toolCall, String result, int stepIndex) {
        if (chatId == null) return;

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("tool");
        message.setContent(result);
        message.setMessageType("observation");
        message.setStatus("completed");

        // 创建工具调用信息
        List<SysAgentChatMessage.ToolCall> toolCalls = new ArrayList<>();
        SysAgentChatMessage.ToolCall call = new SysAgentChatMessage.ToolCall();
        call.setToolName(toolCall.getToolName());

        // 解析参数（假设是 JSON 格式）
        Map<String, Object> params = new HashMap<>();
        params.put("raw", toolCall.getParameters());
        call.setParameters(params);

        call.setResult(result);
        call.setSuccess(true);
        toolCalls.add(call);
        message.setToolCalls(toolCalls);

        chatContextService.addMessage(message);
    }

    /**
     * 创建流式消息响应（用于将字符串内容转换为结构化消息）
     */
    private StreamMessageResponseDto createStreamMessage(String content, String type, boolean isFinish) {
        return StreamMessageResponseDto.builder()
            .message(StreamMessageResponseDto.MessageData.builder()
                .role("assistant")
                .type(type)
                .content(content)
                .contentType("text")
                .messageId(generateMessageId())
                .replyId(userMessageId.get())
                .contentTime(System.currentTimeMillis())
                .build())
            .isFinish(isFinish)
            .index(getAndIncrementMessageIndex())
            .chatId(currentChatId.get())
            .build();
    }

    /**
     * 工具调用指令类
     */
    private static class ToolCallInstruction {
        private String toolName;
        private String parameters;

        public ToolCallInstruction(String toolName, String parameters) {
            this.toolName = toolName;
            this.parameters = parameters;
        }

        public String getToolName() {
            return toolName;
        }

        public String getParameters() {
            return parameters;
        }
    }

    /**
     * 从AI响应中解析工具调用指令
     */
    private ToolCallInstruction parseToolCallFromResponse(String response, List<ToolDto> availableTools) {
        if (!StringUtils.hasText(response) || CollectionUtils.isEmpty(availableTools)) {
            return null;
        }

        // 查找工具调用模式：Action: tool_name
        Pattern actionPattern = Pattern.compile(
            "Action:\\s*([\\w_]+)", Pattern.CASE_INSENSITIVE);
        Matcher actionMatcher = actionPattern.matcher(response);

        if (actionMatcher.find()) {
            String toolName = actionMatcher.group(1);

            // 验证工具是否可用
            boolean toolExists = availableTools.stream()
                .anyMatch(tool -> toolName.equals(tool.getName()));

            if (toolExists) {
                // 查找工具参数：Action Input: parameters
                Pattern inputPattern = Pattern.compile(
                    "Action Input:\\s*(.+?)(?=\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher inputMatcher = inputPattern.matcher(response);

                String parameters = inputMatcher.find() ? inputMatcher.group(1).trim() : "{}";
                return new ToolCallInstruction(toolName, parameters);
            }
        }

        return null;
    }

    /**
     * 执行工具调用
     */
    private String executeToolCall(ToolCallInstruction toolCall) {
        try {
            // 这里应该根据实际的工具调用框架实现
            // 目前返回模拟结果
            log.info("执行工具: {} with parameters: {}", toolCall.getToolName(), toolCall.getParameters());

            // 模拟工具执行
            return String.format("工具 %s 执行成功，参数: %s",
                toolCall.getToolName(), toolCall.getParameters());

        } catch (Exception e) {
            log.error("工具调用失败: {}", toolCall.getToolName(), e);
            return String.format("工具 %s 执行失败: %s", toolCall.getToolName(), e.getMessage());
        }
    }

    /**
     * 检查响应是否包含最终答案
     */
    private boolean containsFinalAnswer(String response) {
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
     */
    private String extractFinalAnswer(String response) {
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
     * 执行流式推理链（带完整响应Future）
     */
    private void performStreamingReasoningChainWithFullResponse(SysAgent agent, List<ToolDto> availableTools,
                                                                String userInput, reactor.core.publisher.FluxSink<String> sink,
                                                                CompletableFuture<String> fullResponseFuture) {
        try {
            // 1. 获取并验证模型配置
            ModelConfigContext modelContext = getAndValidateModelConfigs(agent, sink);
            if (modelContext == null) {
                fullResponseFuture.completeExceptionally(new RuntimeException("模型配置验证失败"));
                return;
            }

            // 2. 创建或获取会话
            SysAgentChat chat = createOrGetChat(agent.getAgentId(), userInput);
            Long chatId = chat.getChatId();

            // 3. 保存用户消息
            saveUserMessage(chatId, userInput);


            // 5. 启动ReAct循环（带完整响应收集）
            startReActLoopWithFullResponse(agent, availableTools, userInput, modelContext,
                sink, fullResponseFuture, 0, new StringBuilder());

        } catch (Exception e) {
            log.error("流式推理执行失败", e);
            fullResponseFuture.completeExceptionally(e);
            sink.error(new RuntimeException("流式推理执行失败: " + e.getMessage()));
        }
    }

    /**
     * 启动ReAct循环（带完整响应收集）
     */
    private void startReActLoopWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                                ModelConfigContext modelContext, reactor.core.publisher.FluxSink<String> sink,
                                                CompletableFuture<String> fullResponseFuture, int currentStep,
                                                StringBuilder accumulatedResponse) {

        // 设置最大循环次数防止无限循环
        final int MAX_STEPS = 10;
        if (currentStep >= MAX_STEPS) {
            sink.next("\n\n⚠️ 已达到最大推理步数限制，停止执行\n");
            fullResponseFuture.complete(accumulatedResponse.toString());
            // 使用handleFinalStepComplete确保增强处理逻辑能执行
            handleFinalStepComplete(accumulatedResponse.toString(), modelContext, sink, response -> {
            });
            return;
        }

        try {
            // 构建当前步骤的提示词
            String cotPrompt = buildPromptForStep(agent, availableTools, userInput, currentStep);

            sink.next(String.format("\n🤔 **推理步骤 %d:**\n", currentStep + 1));

            // 执行流式AI调用
            executeStreamingAICall(cotPrompt, modelContext, sink, stepResponse -> {
                log.info("第{}步获取到完整响应: {}", currentStep + 1, stepResponse);

                // 将当前步骤的响应添加到累积响应中
                accumulatedResponse.append("\n--- 步骤 ").append(currentStep + 1).append(" ---\n")
                    .append(stepResponse).append("\n");

                // 解析响应，判断是否需要执行工具调用
                processReActResponseWithFullResponse(agent, availableTools, userInput, stepResponse,
                    modelContext, sink, fullResponseFuture,
                    currentStep, accumulatedResponse);
            }, false); // 标记为中间步骤

        } catch (Exception e) {
            log.error("ReAct循环执行失败，步骤: {}", currentStep + 1, e);
            fullResponseFuture.completeExceptionally(e);
            sink.error(new RuntimeException("ReAct循环执行失败: " + e.getMessage()));
        }
    }

    /**
     * 处理ReAct响应（带完整响应收集）
     */
    private void processReActResponseWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                                      String aiResponse, ModelConfigContext modelContext,
                                                      reactor.core.publisher.FluxSink<String> sink,
                                                      CompletableFuture<String> fullResponseFuture,
                                                      int currentStep, StringBuilder accumulatedResponse) {

        try {
            // 解析AI响应，查找工具调用指令
            ToolCallInstruction toolCall = parseToolCallFromResponse(aiResponse, availableTools);

            if (toolCall != null) {
                // 需要执行工具调用
                sink.next(String.format("\n🔧 **执行工具:** %s\n", toolCall.getToolName()));
                sink.next(String.format("📝 **参数:** %s\n", toolCall.getParameters()));

                // 执行工具调用
                String toolResult = executeToolCall(toolCall);

                sink.next(String.format("✅ **工具结果:** %s\n", toolResult));

                // 将工具结果添加到累积响应
                accumulatedResponse.append("工具调用: ").append(toolCall.getToolName())
                    .append("\n参数: ").append(toolCall.getParameters())
                    .append("\n结果: ").append(toolResult).append("\n");

                // 将工具执行结果添加到对话历史中，继续下一轮
                appendToolResultAndContinueWithFullResponse(agent, availableTools, userInput, aiResponse,
                    toolCall, toolResult, modelContext, sink,
                    fullResponseFuture, currentStep, accumulatedResponse);

            } else {
                // 没有工具调用，检查是否包含最终答案
                if (containsFinalAnswer(aiResponse)) {
                    sink.next("\n✨ **推理完成**\n");
                    fullResponseFuture.complete(accumulatedResponse.toString());
                    // 使用handleFinalStepComplete确保增强处理逻辑能执行
                    String finalAnswer = extractFinalAnswer(aiResponse);
                    handleFinalStepComplete(finalAnswer, modelContext, sink, response -> {
                    });
                } else {
                    // 继续推理
                    continueReasoningWithFullResponse(agent, availableTools, userInput, aiResponse,
                        modelContext, sink, fullResponseFuture,
                        currentStep, accumulatedResponse);
                }
            }

        } catch (Exception e) {
            log.error("处理ReAct响应失败", e);
            fullResponseFuture.completeExceptionally(e);
            sink.error(new RuntimeException("处理ReAct响应失败: " + e.getMessage()));
        }
    }

    /**
     * 继续推理（带完整响应收集）
     */
    private void continueReasoningWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                                   String previousResponse, ModelConfigContext modelContext,
                                                   reactor.core.publisher.FluxSink<String> sink,
                                                   CompletableFuture<String> fullResponseFuture,
                                                   int currentStep, StringBuilder accumulatedResponse) {

        // 将之前的响应添加到历史记录
        appendToConversationHistory("Assistant", previousResponse);

        // 继续下一轮推理
        startReActLoopWithFullResponse(agent, availableTools, userInput, modelContext, sink,
            fullResponseFuture, currentStep + 1, accumulatedResponse);
    }

    /**
     * 添加工具结果并继续（带完整响应收集）
     */
    private void appendToolResultAndContinueWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                                             String aiResponse, ToolCallInstruction toolCall, String toolResult,
                                                             ModelConfigContext modelContext, reactor.core.publisher.FluxSink<String> sink,
                                                             CompletableFuture<String> fullResponseFuture, int currentStep,
                                                             StringBuilder accumulatedResponse) {

        // 将AI响应和工具执行结果添加到历史记录
        appendToConversationHistory("Assistant", aiResponse);
        appendToConversationHistory("Tool", String.format("工具 %s 执行结果: %s",
            toolCall.getToolName(), toolResult));

        // 继续下一轮推理
        startReActLoopWithFullResponse(agent, availableTools, userInput, modelContext, sink,
            fullResponseFuture, currentStep + 1, accumulatedResponse);
    }

    /**
     * 获取并验证模型配置（用于StreamMessageResponseDto）
     */
    private ModelConfigContext getAndValidateModelConfigsForStreamMessage(SysAgent agent, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink) {
        try {
            SysModelConfigVo mainModelConfig = getModelConfig(agent.getModel(), "主要模型");
            SysModelConfigVo enhanceModelConfig = getModelConfig(agent.getEnhanceModel(), "增强模型");

            // 验证主要模型配置
            if (mainModelConfig == null) {
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    "智能体主要模型配置不存在或无效，无法执行推理任务",
                    currentChatId.get(),
                    generateMessageId(),
                    getAndIncrementMessageIndex()
                ));
                sink.complete();
                return null;
            }

            log.info("智能体主要模型配置获取成功");

            if (enhanceModelConfig != null) {
                log.info("智能体增强模型配置获取成功");
            }

            return new ModelConfigContext(mainModelConfig, enhanceModelConfig);
        } catch (Exception e) {
            log.error("获取模型配置失败", e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "获取模型配置失败: " + e.getMessage(),
                currentChatId.get(),
                generateMessageId(),
                getAndIncrementMessageIndex()
            ));
            sink.complete();
            return null;
        }
    }

    /**
     * 获取并验证模型配置（用于String，保留兼容性）
     */
    private ModelConfigContext getAndValidateModelConfigs(SysAgent agent, reactor.core.publisher.FluxSink<String> sink) {
        try {
            SysModelConfigVo mainModelConfig = getModelConfig(agent.getModel(), "主要模型");
            SysModelConfigVo enhanceModelConfig = getModelConfig(agent.getEnhanceModel(), "增强模型");

            // 验证主要模型配置
            if (mainModelConfig == null) {
                sink.error(new RuntimeException("智能体主要模型配置不存在或无效，无法执行推理任务"));
                return null;
            }

            // 检查模型类型
            if (!isValidModelType(mainModelConfig)) {
                sink.error(new RuntimeException("主要模型不支持聊天功能，请检查模型配置"));
                return null;
            }

            return new ModelConfigContext(mainModelConfig, enhanceModelConfig);

        } catch (Exception e) {
            log.error("查询模型配置失败", e);
            sink.error(new RuntimeException("查询模型配置失败: " + e.getMessage()));
            return null;
        }
    }

    /**
     * 获取模型配置
     */
    private SysModelConfigVo getModelConfig(Long modelId, String modelType) {
        if (modelId == null) {
            log.debug("未配置{}，modelId为null", modelType);
            return null;
        }

        SysModelConfigVo modelConfig = modelConfigService.queryById(modelId);
        if (modelConfig != null) {
            log.info("获取{}配置成功: modelCode={}, provider={}",
                modelType, modelConfig.getModelCode(), modelConfig.getModelProvider());
        } else {
            log.warn("未找到{}配置，模型ID: {}", modelType, modelId);
        }
        return modelConfig;
    }

    /**
     * 验证模型类型是否支持聊天
     */
    private boolean isValidModelType(SysModelConfigVo modelConfig) {
        List<String> modelType = modelConfig.getModelType();
        return modelType != null && (modelType.contains("chat") || modelType.contains("llm"));
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        String promptContent = agent.getPromptContent();
        if (!StringUtils.hasText(promptContent)) {
            // 如果没有自定义提示词，使用默认的ReAct模式提示词
            promptContent = buildDefaultReActPrompt();
        }

        String cotPrompt = promptContent
            .replace("{{agent_personality}}",
                StringUtils.hasText(agent.getAgentPersonality()) ? agent.getAgentPersonality() : "专业助手")
            .replace("{{tools}}",
                CollectionUtils.isEmpty(availableTools) ? "[]" : buildToolListDescription(availableTools))
            .replace("{{input}}", userInput);

        log.info("构建的提示词: {}", cotPrompt);
        return cotPrompt;
    }

    /**
     * 构建默认的ReAct提示词
     */
    private String buildDefaultReActPrompt() {
        return """
            你是一个{{agent_personality}}，能够使用工具来回答问题。请按照以下格式进行推理：

            Question: 用户问题
            Thought: 分析问题，思考需要使用什么工具
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
    private String buildToolListDescription(List<ToolDto> availableTools) {
        if (CollectionUtils.isEmpty(availableTools)) {
            return "[]";
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

        return JSONUtil.toJsonStr(toolList);
    }

    /**
     * 执行流式AI调用（带步骤类型参数）
     */
    private void executeStreamingAICall(String cotPrompt, ModelConfigContext modelContext,
                                        reactor.core.publisher.FluxSink<String> sink,
                                        Consumer<String> fullResponseCallback,
                                        boolean isFinalStep) {
        // 获取聊天服务
        IChatService chatService = aiService.getChatService(modelContext.getMainModel().getModelProvider());
        if (chatService == null) {
            sink.error(new RuntimeException("无法获取聊天服务，提供商: " + modelContext.getMainModel().getModelProvider()));
            return;
        }

        // 构建聊天请求
        IChatRequest chatRequest = buildChatRequest(cotPrompt, modelContext.getMainModel());

        // 执行流式调用
        Flux<IChatResponse> modelStream = chatService.stream(chatRequest);
        processModelStream(modelStream, modelContext, sink, fullResponseCallback, isFinalStep);

    }

    /**
     * 执行流式AI调用（用于StreamMessageResponseDto）
     */
    private void executeStreamingAICallForMessage(String cotPrompt, ModelConfigContext modelContext,
                                                  reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                  Consumer<String> fullResponseCallback,
                                                  boolean isFinalStep) {
        // 获取聊天服务
        IChatService chatService = aiService.getChatService(modelContext.getMainModel().getModelProvider());
        if (chatService == null) {
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "无法获取聊天服务，提供商: " + modelContext.getMainModel().getModelProvider(),
                currentChatId.get(),
                generateMessageId(),
                getAndIncrementMessageIndex()
            ));
            sink.complete();
            return;
        }

        // 构建聊天请求
        IChatRequest chatRequest = buildChatRequest(cotPrompt, modelContext.getMainModel());

        chatRequest.setStop(Collections.singletonList("Observation"));
        // 执行流式调用
        Flux<IChatResponse> modelStream = chatService.stream(chatRequest);
        processModelStreamForMessage(modelStream, modelContext, sink, fullResponseCallback, isFinalStep);
    }

    /**
     * 构建聊天请求
     */
    private IChatRequest buildChatRequest(String prompt, SysModelConfigVo modelConfig) {
        IChatRequest request = new IChatRequest();
        request.setStream(true);
        request.setModel(modelConfig.getModelCode());
        request.setApiKey(modelConfig.getApiKey());
        request.setBaseUrl(modelConfig.getBaseUrl());
        request.setPrompt(prompt);
        return request;
    }


    /**
     * 处理模型流式响应
     */
    private void processModelStream(Flux<IChatResponse> modelStream, ModelConfigContext modelContext,
                                    reactor.core.publisher.FluxSink<String> sink,
                                    java.util.function.Consumer<String> fullResponseCallback,
                                    boolean isFinalStep) {
        StringBuilder fullResponse = new StringBuilder();

        modelStream
            .doOnNext(chunk -> handleStreamChunk(chunk, sink, fullResponse))
            .doOnComplete(() -> {
                if (isFinalStep) {
                    handleFinalStepComplete(fullResponse.toString(), modelContext, sink, fullResponseCallback);
                } else {
                    handleIntermediateStepComplete(fullResponse.toString(), sink, fullResponseCallback);
                }
            })
            .doOnError(error -> handleStreamError(error, sink))
            .subscribe();
    }

    /**
     * 处理模型流式响应（用于StreamMessageResponseDto）
     */
    private void processModelStreamForMessage(Flux<IChatResponse> modelStream, ModelConfigContext modelContext,
                                              reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                              java.util.function.Consumer<String> fullResponseCallback,
                                              boolean isFinalStep) {
        StringBuilder fullResponse = new StringBuilder();

        modelStream
            .doOnNext(chunk -> handleStreamChunkForMessage(chunk, sink, fullResponse))
            .doOnComplete(() -> {
                if (isFinalStep) {
                    handleFinalStepCompleteForMessage(fullResponse.toString(), modelContext, sink, fullResponseCallback);
                } else {
                    handleIntermediateStepCompleteForMessage(fullResponse.toString(), sink, fullResponseCallback);
                }
            })
            .doOnError(error -> handleStreamErrorForMessage(error, sink))
            .subscribe();
    }

    /**
     * 处理流式响应块
     */
    private void handleStreamChunk(IChatResponse chunk, reactor.core.publisher.FluxSink<String> sink,
                                   StringBuilder fullResponse) {
        if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
            String text = chunk.getResult().getOutput().getText();
            if (StringUtils.hasText(text)) {
                sink.next(text);
                fullResponse.append(text);
            }
        }
    }

    /**
     * 处理流式响应块（用于StreamMessageResponseDto）
     */
    private void handleStreamChunkForMessage(IChatResponse chunk, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                             StringBuilder fullResponse) {
        if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
            String text = chunk.getResult().getOutput().getText();
            if (StringUtils.hasText(text)) {
                sink.next(createStreamMessage(text, "thought", false));
                fullResponse.append(text);
            }
        }
    }


    /**
     * 处理流式响应完成
     */
    /**
     * 处理中间步骤的流式响应完成
     */
    private void handleIntermediateStepComplete(String stepResponse,
                                                reactor.core.publisher.FluxSink<String> sink,
                                                java.util.function.Consumer<String> fullResponseCallback) {
        log.info("ReAct中间步骤流式调用完成");
        // 调用回调函数继续处理，但不关闭流
        fullResponseCallback.accept(stepResponse);
        // 注意：这里不调用 sink.complete()，保持流开放
    }

    /**
     * 处理最终步骤的流式响应完成（包含增强和关闭流）
     */
    private void handleFinalStepComplete(String finalResponse, ModelConfigContext modelContext,
                                         reactor.core.publisher.FluxSink<String> sink,
                                         java.util.function.Consumer<String> fullResponseCallback) {
        log.info("ReAct最终步骤流式调用完成");
        fullResponseCallback.accept(finalResponse); // 调用回调函数

        // 如果配置了增强模型，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)) {
            try {
                // 明确告知客户端还有增强内容
                sink.next("\n\n[ENHANCING] 正在优化回复...\n");
                String enhancedResponse = enhanceResponse(finalResponse, modelContext.getEnhanceModel(), null);

                if (!enhancedResponse.equals(finalResponse)) {
                    sink.next("[ENHANCED]\n");
                    sink.next(enhancedResponse);
                    sink.next("\n[ENHANCEMENT_COMPLETE]\n");
                } else {
                    sink.next("[ENHANCEMENT_SKIPPED] 优化后内容与原始内容相同\n");
                }
            } catch (Exception e) {
                log.error("增强模型处理失败", e);
                sink.next("[ENHANCEMENT_FAILED] 增强失败，使用原始回复\n");
            }
        }

        // 发送完成标记并关闭流
        sink.next("[DONE]");
        sink.complete();
    }

    /**
     * 处理流式响应错误
     */
    private void handleStreamError(Throwable error, reactor.core.publisher.FluxSink<String> sink) {
        log.error("AI模型流式调用失败", error);
        sink.error(new RuntimeException("AI模型调用失败: " + error.getMessage()));
    }

    /**
     * 处理流式响应错误（用于StreamMessageResponseDto）
     */
    private void handleStreamErrorForMessage(Throwable error, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink) {
        log.error("AI模型流式调用失败", error);
        sink.next(StreamMessageResponseDto.createErrorMessage(
            "AI模型调用失败: " + error.getMessage(),
            currentChatId.get(),
            generateMessageId(),
            getAndIncrementMessageIndex()
        ));
        sink.complete();
    }

    /**
     * 处理中间步骤的流式响应完成（用于StreamMessageResponseDto）
     */
    private void handleIntermediateStepCompleteForMessage(String stepResponse,
                                                          reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                          java.util.function.Consumer<String> fullResponseCallback) {
        log.info("ReAct中间步骤流式调用完成");
        // 调用回调函数继续处理，但不关闭流
        fullResponseCallback.accept(stepResponse);
        // 注意：这里不调用 sink.complete()，保持流开放
    }

    /**
     * 处理最终步骤的流式响应完成（用于StreamMessageResponseDto）
     */
    private void handleFinalStepCompleteForMessage(String finalResponse, ModelConfigContext modelContext,
                                                   reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                   java.util.function.Consumer<String> fullResponseCallback) {
        log.info("ReAct最终步骤流式调用完成");
        fullResponseCallback.accept(finalResponse); // 调用回调函数

        // 如果配置了增强模型，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)) {
            try {
                // 明确告知客户端还有增强内容
                sink.next(createStreamMessage("\n\n[ENHANCING] 正在优化回复...\n", "thought", false));
                String enhancedResponse = enhanceResponse(finalResponse, modelContext.getEnhanceModel(), null);

                if (!enhancedResponse.equals(finalResponse)) {
                    sink.next(createStreamMessage("[ENHANCED]\n", "thought", false));
                    sink.next(createStreamMessage(enhancedResponse, "answer", false));
                    sink.next(createStreamMessage("\n[ENHANCEMENT_COMPLETE]\n", "thought", false));
                } else {
                    sink.next(createStreamMessage("[ENHANCEMENT_SKIPPED] 优化后内容与原始内容相同\n", "thought", false));
                }
            } catch (Exception e) {
                log.error("增强模型处理失败", e);
                sink.next(createStreamMessage("[ENHANCEMENT_FAILED] 增强失败，使用原始回复\n", "thought", false));
            }
        }

        // 发送完成消息
        sink.next(StreamMessageResponseDto.createFinishMessage(
            currentChatId.get(),
            generateMessageId(),
            getAndIncrementMessageIndex()
        ));
        sink.complete();
    }

    /**
     * 模型配置上下文类
     */
    private static class ModelConfigContext {
        private final SysModelConfigVo mainModel;
        private final SysModelConfigVo enhanceModel;

        public ModelConfigContext(SysModelConfigVo mainModel, SysModelConfigVo enhanceModel) {
            this.mainModel = mainModel;
            this.enhanceModel = enhanceModel;
        }

        public SysModelConfigVo getMainModel() {
            return mainModel;
        }

        public SysModelConfigVo getEnhanceModel() {
            return enhanceModel;
        }
    }

    @Override
    public boolean checkExitCondition(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        String lowerInput = input.toLowerCase().trim();
        return lowerInput.equals("exit") ||
            lowerInput.equals("quit") ||
            lowerInput.equals("退出") ||
            lowerInput.equals("结束");
    }

    /**
     * 使用增强模型优化回复
     */
    private String enhanceResponse(String originalResponse, SysModelConfigVo enhanceModelConfig, SysAgent agent) {
        try {
            log.info("使用增强模型优化回复: {}", enhanceModelConfig.getModelCode());

            // 构建增强提示词
            String enhancePrompt = "请优化以下回复内容，使其更加准确、友好和有用：\n\n" + originalResponse;

            // 获取增强模型的聊天服务
            IChatService enhanceChatService = aiService.getChatService(enhanceModelConfig.getModelProvider());
            if (enhanceChatService == null) {
                log.warn("无法获取增强模型的聊天服务: {}", enhanceModelConfig.getModelProvider());
                return originalResponse;
            }

            // 构建增强模型请求
            IChatRequest enhanceRequest = new IChatRequest();
            enhanceRequest.setStream(false); // 增强模型使用同步调用
            enhanceRequest.setModel(enhanceModelConfig.getModelCode());
            enhanceRequest.setApiKey(enhanceModelConfig.getApiKey());
            enhanceRequest.setBaseUrl(enhanceModelConfig.getBaseUrl());
            enhanceRequest.setPrompt(enhancePrompt);

            // 调用增强模型
//            String enhancedResponse = enhanceChatService.chat(enhanceRequest);

            if (StringUtils.hasText("11")) {
                log.info("增强模型优化成功");
                return "11";
            } else {
                log.warn("增强模型返回空结果");
                return originalResponse;
            }

        } catch (Exception e) {
            log.error("增强模型优化失败，返回原始回复", e);
            return originalResponse;
        }
    }

    /**
     * 流式响应结果包装器
     */
    public static class StreamResult {
        private final Flux<String> stream;
        private final CompletableFuture<String> fullResponseFuture;

        public StreamResult(Flux<String> stream, CompletableFuture<String> fullResponseFuture) {
            this.stream = stream;
            this.fullResponseFuture = fullResponseFuture;
        }

        /**
         * 获取流式响应
         */
        public Flux<String> getStream() {
            return stream;
        }

        /**
         * 获取完整响应（异步）
         */
        public CompletableFuture<String> getFullResponseFuture() {
            return fullResponseFuture;
        }

        /**
         * 获取完整响应（同步，会阻塞）
         */
        public String getFullResponse() {
            try {
                return fullResponseFuture.get();
            } catch (Exception e) {
                throw new RuntimeException("获取完整响应失败", e);
            }
        }

        /**
         * 获取完整响应（同步，带超时）
         */
        public String getFullResponse(long timeout, TimeUnit unit) {
            try {
                return fullResponseFuture.get(timeout, unit);
            } catch (Exception e) {
                throw new RuntimeException("获取完整响应失败", e);
            }
        }
    }

    /**
     * 执行自由对话模式的流式处理
     */
    @Override
    public Flux<StreamMessageResponseDto> executeFreeChatStream(SysAgent agent, String userInput) {
        return Flux.<StreamMessageResponseDto>create(sink -> {
            try {
                SaTokenContext context = SaHolder.getContext();
                SaTokenContextMockUtil.setMockContext(() -> {
                    SaManager.setSaTokenContext(context);
                });
                // 清理线程本地变量
                clearThreadLocals();

                // 创建简单的自由对话流式处理
                String chatIdStr = "chat_" + System.currentTimeMillis();
                String userMsgId = generateMessageId();
                setCurrentChatInfo(chatIdStr, userMsgId);

                // 发送开始消息
                sink.next(createStreamMessage("开始处理您的问题...", "thought", false));

                // 构建提示词
                String prompt = buildFreeChatPrompt(agent, userInput);

                // 发送回答
                String response = "这是一个模拟的回答。实际实现中应该调用AI模型。";
                sink.next(createStreamMessage(response, "answer", false));

                // 发送完成消息
                sink.next(StreamMessageResponseDto.createFinishMessage(chatIdStr, generateMessageId(), getAndIncrementMessageIndex()));
                sink.complete();

            } catch (Exception e) {
                log.error("自由对话流式执行失败", e);
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    e.getMessage(),
                    currentChatId.get(),
                    generateMessageId(),
                    getAndIncrementMessageIndex()
                ));
                sink.complete();
            }
        }).doFinally(signal -> clearThreadLocals());
    }

    /**
     * 执行自由对话模式的流式处理（带完整响应）
     */
    @Override
    public StreamResult executeFreeChatStreamWithFullResponse(SysAgent agent, String userInput) {
        CompletableFuture<String> fullResponseFuture = new CompletableFuture<>();

        Flux<String> stream = Flux.create(sink -> {
            try {
                SaTokenContext context = SaHolder.getContext();
                SaTokenContextMockUtil.setMockContext(() -> {
                    SaManager.setSaTokenContext(context);
                });
                // 执行自由对话的流式处理，并在完成时设置完整响应
                performFreeChatStreamingWithFullResponse(agent, userInput, sink, fullResponseFuture);
            } catch (Exception e) {
                log.error("自由对话流式执行失败", e);
                fullResponseFuture.completeExceptionally(e);
                sink.error(e);
            }
        });

        return new StreamResult(stream, fullResponseFuture);
    }


    /**
     * 执行自由对话的流式处理（带完整响应）
     */
    private void performFreeChatStreamingWithFullResponse(SysAgent agent, String userInput,
                                                          reactor.core.publisher.FluxSink<String> sink,
                                                          CompletableFuture<String> fullResponseFuture) {
        try {
            // 1. 获取并验证模型配置
            ModelConfigContext modelContext = getAndValidateModelConfigs(agent, sink);
            if (modelContext == null) {
                fullResponseFuture.complete("");
                return;
            }

            // 2. 创建或获取会话
            SysAgentChat chat = createOrGetChat(agent.getAgentId(), userInput);
            Long chatId = chat.getChatId();

            // 3. 保存用户消息
            saveUserMessage(chatId, userInput);

            // 4. 构建对话提示词
            String prompt = buildFreeChatPrompt(agent, userInput);

            // 5. 直接执行流式AI调用
            executeStreamingAICall(prompt, modelContext, sink, fullResponse -> {
                log.info("自由对话完成，响应长度: {}", fullResponse.length());

                // 保存AI响应到数据库
                saveAssistantMessage(chatId, fullResponse, "text");

                // 设置完整响应
                fullResponseFuture.complete(fullResponse);

                // 处理最终的响应
                handleFinalStepComplete(fullResponse, modelContext, sink, response -> {
                    log.info("自由对话流式处理完成（带完整响应）");
                });
            }, true);

        } catch (Exception e) {
            log.error("自由对话流式处理失败", e);
            fullResponseFuture.completeExceptionally(e);
            sink.error(new RuntimeException("自由对话流式处理失败: " + e.getMessage()));
        }
    }

    /**
     * 构建自由对话的提示词
     */
    private String buildFreeChatPrompt(SysAgent agent, String userInput) {
        StringBuilder prompt = new StringBuilder();

        // 获取智能体人设，如果为空则使用默认值
        String agentPersonality = StringUtils.hasText(agent.getAgentPersonality()) ?
            agent.getAgentPersonality() : "智能助手";

        // 添加系统角色设定
        prompt.append("你是一个").append(agentPersonality).append("。\n\n");

        // 添加自定义提示词
        if (StringUtils.hasText(agent.getPromptContent())) {
            String processedPrompt = agent.getPromptContent()
                .replace("{{agent_personality}}", agentPersonality)
                .replace("{{query}}", userInput);
            prompt.append(processedPrompt).append("\n\n");
        }

        // 添加用户输入
        prompt.append("用户问题：").append(userInput);

        return prompt.toString();
    }

    /**
     * 保存助手消息到数据库
     */
    private void saveAssistantMessage(Long chatId, String content, String messageType) {
        try {
            SysAgentChatMessage message = new SysAgentChatMessage();
            message.setChatId(chatId);
            message.setRole("assistant");
            message.setContent(content);
            message.setMessageType(messageType);
            message.setStatus("completed");

            chatContextService.addMessage(message);
            log.debug("成功保存助手消息到会话: {}", chatId);
        } catch (Exception e) {
            log.error("保存助手消息失败，会话ID: {}", chatId, e);
        }
    }
}
