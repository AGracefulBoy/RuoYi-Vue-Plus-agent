package org.dromara.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.mock.SaTokenContextMockUtil;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.config.PythonProperties;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.domain.bo.PythonDebugRequestBo;
import org.dromara.system.domain.dto.HitDocumentDTO;
import org.dromara.system.domain.dto.HitSourceDTO;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.TokenUsageDto;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.domain.vo.SysToolVo;
import org.dromara.system.service.*;
import org.dromara.system.service.helper.PromptBuilderHelper;
import org.dromara.system.service.helper.StreamMessageBuilder;
import org.dromara.system.service.tool.executor.IToolExecutor;
import org.dromara.system.service.tool.executor.ToolExecutionResult;
import org.dromara.system.domain.context.ModelConfigContext;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.instruction.ToolCallInstruction;
import org.dromara.system.util.HttpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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

    @Autowired
    private List<IToolExecutor> toolExecutors;

    @Autowired
    private ISysToolService toolService;

    @Autowired
    private ISysPythonPackageService pythonPackageService;

    @Autowired
    private PythonProperties pythonProperties;

    @Autowired
    private IElasticsearchDocumentService elasticsearchDocumentService;

    @Autowired
    private PromptBuilderHelper promptBuilderHelper;

    @Autowired
    private StreamMessageBuilder streamMessageBuilder;

    @Override
    public Flux<StreamMessageResponseDto> executeReActStream(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        SaTokenContext context = SaHolder.getContext();

        return Flux.<StreamMessageResponseDto>create(sink -> {
            try {
                SaTokenContextMockUtil.setMockContext(() -> {
                    SaManager.setSaTokenContext(context);
                });
                // 创建StreamingContext
                StreamingContext ctx = new StreamingContext();
                // 执行流式推理
                performStreamingReasoningChain(agent, availableTools, userInput, sink, ctx);

            } catch (Exception e) {
                log.error("流式ReAct执行失败", e);
                // 发送错误消息
                StreamingContext errorCtx = new StreamingContext();
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    e.getMessage(),
                    errorCtx.getCurrentChatId(),
                    generateMessageId(),
                    errorCtx.getAndIncrementMessageIndex()
                ));
                sink.complete();
            }
        });
    }


    /**
     * 执行流式推理链
     */
    private void performStreamingReasoningChain(SysAgent agent, List<ToolDto> availableTools,
                                                String userInput, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                StreamingContext ctx) {
        try {
            // 保存工具列表到上下文
            ctx.setCurrentAvailableTools(availableTools);
            // 1. 获取并验证模型配置
            ModelConfigContext modelContext = getAndValidateModelConfigsForStreamMessage(agent, sink, ctx);
            if (modelContext == null) return;

            // 2. 创建或获取会话
            SysAgentChat chat = createOrGetChat(agent.getAgentId(), userInput);
            Long chatId = chat.getChatId();
            String chatIdStr = String.valueOf(chatId);

            // 设置会话信息和用户消息ID
            String userMsgId = generateMessageId();
            ctx.setCurrentChatId(chatIdStr);
            ctx.setUserMessageId(userMsgId);

            // 3. 保存用户消息
            saveUserMessage(chatId, userInput);

            // 5. 启动ReAct循环
            startReActLoopWithChat(agent, availableTools, userInput, modelContext, sink, 0, chatId, ctx);

        } catch (Exception e) {
            log.error("流式推理执行失败", e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "流式推理执行失败: " + e.getMessage(),
                ctx.getCurrentChatId(),
                generateMessageId(),
                ctx.getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 启动ReAct循环（带会话管理）
     */
    private void startReActLoopWithChat(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                        ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                        int currentStep, Long chatId, StreamingContext ctx) {
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep, chatId, ctx);
    }

    /**
     * 启动ReAct循环（核心实现）
     */
    private void startReActLoop(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                int currentStep, Long chatId, StreamingContext ctx) {

        // 设置最大循环次数防止无限循环
        final int MAX_STEPS = 20;
        if (currentStep >= MAX_STEPS) {
            sink.next(createStreamMessage("\n\n⚠️ 已达到最大推理步数限制，停止执行\n", "answer", false, ctx));
            // 使用handleFinalStepCompleteForMessage确保增强处理逻辑能执行
            handleFinalStepCompleteForMessage("已达到最大推理步数限制", modelContext, sink, response -> {
            }, ctx);
            return;
        }

        try {
            // 构建当前步骤的提示词
            String cotPrompt = buildPromptForStep(agent, availableTools, userInput, currentStep, ctx);

            // 记录每一步的完整提示词以便调试
            log.info("第{}步 - 构建的完整提示词: {}", currentStep + 1, cotPrompt);

            // 将完整提示词添加到提示词链中
            ctx.getPromptChain()
                .append(cotPrompt)
                .append("\n");

            // 执行流式AI调用
            executeStreamingAICallForMessage(cotPrompt, modelContext, sink, fullResponse -> {
                log.info("第{}步获取到完整响应: {}", currentStep + 1, fullResponse);

                // 解析响应，判断是否需要执行工具调用
                processReActResponse(agent, availableTools, userInput, fullResponse,
                    modelContext, sink, currentStep, chatId, ctx);
            }, false, currentStep, ctx); // 标记为中间步骤，传递currentStep和ctx

        } catch (Exception e) {
            log.error("ReAct循环执行失败，步骤: {}", currentStep + 1, e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "ReAct循环执行失败: " + e.getMessage(),
                ctx.getCurrentChatId(),
                generateMessageId(),
                ctx.getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 处理ReAct响应
     */
    private void processReActResponse(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                      String aiResponse, ModelConfigContext modelContext,
                                      reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, int currentStep, Long chatId, StreamingContext ctx) {

        try {
            // 将AI响应添加到提示词链中
            ctx.getPromptChain()
                .append(String.format("\n=== Step %d Response ===\n", currentStep + 1))
                .append(aiResponse)
                .append("\n");

            if (containsFinalAnswer(aiResponse)) {
                // 将包含Final Answer的完整响应添加到历史记录
                appendToConversationHistory("Assistant", aiResponse, ctx);

                // 提取最终答案
                String finalAnswer = extractFinalAnswer(aiResponse);

                // 判断是否需要增强回复
                if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalAnswer)
                    && !modelContext.getMainModel().getModelId().equals(modelContext.getEnhanceModel().getModelId())) {
                    // 需要增强回复
                    sink.next(createStreamMessage("\n\n🎯 **增强回复...**\n", "enhancing", false, ctx));
                    executeEnhanceStreamingCall(finalAnswer, modelContext, sink, ctx);
                } else {
                    // 不需要增强，直接输出最终答案
                    sink.next(createStreamMessage("\n✨ **推理完成**\n", "answer", false, ctx));
                    handleFinalStepCompleteForMessage(finalAnswer, modelContext, sink, response -> {
                    }, ctx);
                }
                // 重要：检测到Final Answer后终止循环
                return;
            }
            // 解析AI响应，查找工具调用指令
            ToolCallInstruction toolCall = parseToolCallFromResponse(aiResponse, availableTools);
            if (toolCall != null) {
                // 保存思考步骤（包含工具调用决策）
                saveThoughtStep(chatId, aiResponse, "action", currentStep);

                // 需要执行工具调用
                sink.next(createStreamMessage(String.format("\n🔧 **执行工具:** %s\n", toolCall.getToolName()), "action", false, ctx));
                sink.next(createStreamMessage(String.format("📝 **参数:** %s\n", toolCall.getParameters()), "action", false, ctx));

                // 执行工具调用
                String toolResult = executeToolCall(toolCall, sink, ctx);

                // 如果不是流式输出，显示工具结果
                if (!isStreamingTool(toolCall)) {
                    sink.next(createStreamMessage(String.format("✅ **工具结果:** %s\n", toolResult), "observation", false, ctx));
                }

                // 保存工具执行结果
                saveToolResult(chatId, toolCall, toolResult, currentStep);

                // 将工具执行结果添加到对话历史中，继续下一轮
                appendToolResultAndContinue(agent, availableTools, userInput, aiResponse,
                    toolCall, toolResult, modelContext, sink, currentStep, chatId, ctx);

            } else {
                // 没有工具调用，检查是否包含最终答案
                if (containsFinalAnswer(aiResponse)) {
                    // 将包含Final Answer的完整响应添加到历史记录
                    appendToConversationHistory("Assistant", aiResponse, ctx);

                    // 提取最终答案
                    String finalAnswer = extractFinalAnswer(aiResponse);

                    // 判断是否需要增强回复
                    if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalAnswer)
                        && !modelContext.getMainModel().getModelId().equals(modelContext.getEnhanceModel().getModelId())) {
                        // 需要增强回复
                        sink.next(createStreamMessage("\n\n🎯 **增强回复...**\n", "enhancing", false, ctx));
                        executeEnhanceStreamingCall(finalAnswer, modelContext, sink, ctx);
                    } else {
                        // 不需要增强，直接输出最终答案
                        sink.next(createStreamMessage("\n✨ **推理完成**\n", "answer", false, ctx));
                        handleFinalStepCompleteForMessage(finalAnswer, modelContext, sink, response -> {
                        }, ctx);
                    }
                } else {
                    // 保存思考步骤
                    saveThoughtStep(chatId, aiResponse, "action", currentStep);

                    // 继续推理
                    continueReasoning(agent, availableTools, userInput, aiResponse,
                        modelContext, sink, currentStep, chatId, ctx);
                }
            }

        } catch (Exception e) {
            log.error("处理ReAct响应失败", e);
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "处理ReAct响应失败: " + e.getMessage(),
                ctx.getCurrentChatId(),
                generateMessageId(),
                ctx.getAndIncrementMessageIndex()
            ));
            sink.complete();
        }
    }

    /**
     * 执行工具调用
     *
     * @param toolCall 工具调用指令
     * @param sink 流式输出sink
     * @param ctx 流式上下文
     * @return 执行结果
     */
    private String executeToolCall(ToolCallInstruction toolCall, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        try {
            String type = toolCall.getType();
            if ("tool".equals(type)) {
                // 查询工具配置
                SysToolVo tool = toolService.queryById(toolCall.getId());
                if (tool == null) {
                    return "工具不存在: " + toolCall.getToolName();
                }

                // 根据工具类型执行
                if ("2".equals(tool.getToolType())) {
                    // Python脚本执行
                    return executePythonScript(tool, toolCall.getParameters(), sink, ctx);
                } else {
                    // API工具或其他类型
                    for (IToolExecutor executor : toolExecutors) {
                        if (executor.supports(tool.getToolType())) {
                            ToolExecutionResult result = executor.execute(tool, toolCall.getParameters());
                            return result.isSuccess() ?
                                String.valueOf(result.getData()) :
                                "执行失败: " + result.getErrorMessage();
                        }
                    }
                    return "未找到支持的执行器: " + tool.getToolType();
                }
            } else if ("knowledge".equals(type)) {
                // 实现知识库查询功能
                try {
                    // 解析参数，获取查询问题
                    String parameters = toolCall.getParameters();
                    String question = parameters;
                    String metadata = null;

                    // 如果参数是JSON格式，尝试解析
                    if (parameters != null && parameters.trim().startsWith("{")) {
                        Map<String, Object> params = JSONUtil.toBean(parameters, Map.class);
                        question = (String) params.getOrDefault("question", parameters);
                        metadata = params.containsKey("metadata") ? JSONUtil.toJsonStr(params.get("metadata")) : null;
                    }

                    // 调用ElasticsearchDocumentService进行混合搜索
                    List<HitSourceDTO> hitSourceDTOS = elasticsearchDocumentService.hybridSearch(
                        toolCall.getId(), question, metadata, true);

                    // 格式化搜索结果
                    if (hitSourceDTOS == null || hitSourceDTOS.isEmpty()) {
                        return "未找到相关文档";
                    }

                    List<HashMap<String, String>> resultHit = new ArrayList<>();
                    for (HitSourceDTO hitSourceDTO : hitSourceDTOS) {
                        HitDocumentDTO doc = hitSourceDTO.getHitDocument();
                        if (doc != null) {
                            HashMap<String, String> hitSourceMap = new HashMap<>();
                            hitSourceMap.put("content", doc.getContent());
                            hitSourceMap.put("metadata", doc.getMetadata());
                            hitSourceMap.put("embeddingContent", doc.getEmbeddingContent());
                            resultHit.add(hitSourceMap);
                        }
                    }

                    return JSONUtil.toJsonStr(resultHit);
                } catch (Exception e) {
                    log.error("执行知识库查询失败", e);
                    return "知识库查询失败: " + e.getMessage();
                }
            } else if ("datasource".equals(type)) {
                // 符合YAGNI原则，暂时返回占位信息
                return "数据源查询功能待实现";
            } else {
                return "不支持的工具类型: " + type;
            }
        } catch (Exception e) {
            log.error("执行工具调用失败", e);
            return "执行异常: " + e.getMessage();
        }
    }

    /**
     * 执行Python脚本
     *
     * @param tool       工具配置
     * @param parameters 参数（JSON格式）
     * @param sink      流式输出sink（可选，用于流式输出）
     * @param ctx       流式上下文（可选，用于流式输出）
     * @return 执行结果
     */
    private String executePythonScript(SysToolVo tool, String parameters, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        try {
            // 构建Python调试请求
            PythonDebugRequestBo request = new PythonDebugRequestBo();
            request.setCode(tool.getScriptCode());
            request.setFunctionName(tool.getFunctionName());
            request.setStream("1".equals(tool.getIsStream()));

            // 解析参数
            if (StringUtils.hasText(parameters)) {
                Map<String, Object> params = JSONUtil.toBean(parameters, Map.class);
                request.setParams(params);
            }

            // 构建请求数据
            Map<String, String> data = new HashMap<>();
            data.put("code", request.getCode());
            data.put("func_name", request.getFunctionName());
            data.put("params", parameters != null ? parameters : "{}");

            String requestJson = JSONUtil.toJsonStr(data);

            // 检查是否需要流式输出
            if ("1".equals(tool.getIsStream()) && sink != null && ctx != null) {
                // 流式输出模式
                log.info("Python脚本流式执行开始，工具：{}", tool.getToolName());

                // 用于收集所有输出内容
                StringBuilder fullOutput = new StringBuilder();

                // 创建自定义输出流，将数据写入FluxSink
                OutputStream streamingOutput = new OutputStream() {
                    private final ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

                    @Override
                    public void write(int b) throws IOException {
                        // 当遇到换行符时（字节值为10），发送一行数据
                        if (b == '\n') {
                            // 将累积的字节转换为UTF-8字符串（不包含换行符）
                            String line = lineBuffer.toString(StandardCharsets.UTF_8);
                            // 发送流式消息
                            sink.next(createStreamMessage(line, "observation", false, ctx));
                            // 将内容添加到完整输出中，加上换行符以保持原始格式
                            fullOutput.append(line).append('\n');
                            // 清空缓冲区
                            lineBuffer.reset();
                        } else {
                            // 非换行符，添加到缓冲区
                            lineBuffer.write(b);
                        }
                    }

                    @Override
                    public void flush() throws IOException {
                        // 如果缓冲区还有数据，发送出去
                        if (lineBuffer.size() > 0) {
                            // 将累积的字节转换为UTF-8字符串
                            String remainingData = lineBuffer.toString(StandardCharsets.UTF_8);
                            sink.next(createStreamMessage(remainingData, "observation", false, ctx));
                            // 将内容添加到完整输出中
                            fullOutput.append(remainingData);
                            lineBuffer.reset();
                        }
                    }
                };

                // 使用流式API
                String streamUrl = pythonProperties.getApi().getStreamUrl();
                boolean success = HttpUtils.sendPostStream(streamUrl, requestJson, streamingOutput);

                if (success) {
                    log.info("Python脚本流式执行完成，工具：{}，完整输出：{}", tool.getToolName(), fullOutput.toString());
                    // 返回完整的输出内容
                    return fullOutput.toString();
                } else {
                    log.error("Python脚本流式执行失败，工具：{}", tool.getToolName());
                    // 如果有部分输出，也返回
                    if (fullOutput.length() > 0) {
                        return fullOutput.toString();
                    }
                    return String.format("Python脚本 %s 执行失败", tool.getToolName());
                }
            } else {
                // 非流式输出模式（原有逻辑）
                String url = pythonProperties.getApi().getExecUrl();
                String result = HttpUtils.sendPost(url, requestJson);

                log.info("Python脚本执行完成，工具：{}，结果：{}", tool.getToolName(), result);

                // 直接返回结果（Python服务返回的是执行结果的字符串）
                return result;
            }

        } catch (Exception e) {
            log.error("执行Python脚本失败，工具：{}", tool.getToolName(), e);
            return "Python脚本执行异常: " + e.getMessage();
        }
    }

    /**
     * 检查工具是否配置为流式输出
     *
     * @param toolCall 工具调用指令
     * @return 是否为流式输出工具
     */
    private boolean isStreamingTool(ToolCallInstruction toolCall) {
        try {
            if ("tool".equals(toolCall.getType())) {
                SysToolVo tool = toolService.queryById(toolCall.getId());
                if (tool != null && "2".equals(tool.getToolType())) {
                    // Python脚本类型且配置为流式输出
                    return "1".equals(tool.getIsStream());
                }
            }
        } catch (Exception e) {
            log.error("检查工具流式配置失败", e);
        }
        return false;
    }

    /**
     * 构建指定步骤的提示词
     */
    private String buildPromptForStep(SysAgent agent, List<ToolDto> availableTools,
                                      String userInput, int step, StreamingContext ctx) {
        return promptBuilderHelper.buildPromptForStep(agent, availableTools, userInput, step, getConversationHistory(ctx));
    }

    /**
     * 继续推理（没有工具调用的情况）
     */
    private void continueReasoning(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                   String previousResponse, ModelConfigContext modelContext,
                                   reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, int currentStep, Long chatId, StreamingContext ctx) {

        // 将之前的响应添加到历史记录
        appendToConversationHistory("Assistant", previousResponse, ctx);

        // 继续下一轮推理
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep + 1, chatId, ctx);
    }

    /**
     * 添加工具结果并继纭
     */
    private void appendToolResultAndContinue(SysAgent agent, List<ToolDto> availableTools, String userInput,
                                             String aiResponse, ToolCallInstruction toolCall, String toolResult,
                                             ModelConfigContext modelContext, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                             int currentStep, Long chatId, StreamingContext ctx) {

        // 将AI响应和工具执行结果添加到历史记录
        appendToConversationHistory("Assistant", aiResponse, ctx);
        appendToConversationHistory("Observation", toolResult, ctx);

        // 记录当前对话历史以便调试
        log.debug("第{}步 - 工具执行后的对话历史: {}", currentStep + 1, getConversationHistory(ctx));

        // 继续下一轮推理
        startReActLoop(agent, availableTools, userInput, modelContext, sink, currentStep + 1, chatId, ctx);
    }


    /**
     * 添加到对话历史
     */
    private void appendToConversationHistory(String role, String content, StreamingContext ctx) {
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
     */
    private String getConversationHistory(StreamingContext ctx) {
        return ctx.getConversationHistory().toString();
    }


    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return String.valueOf(System.currentTimeMillis() + (int) (Math.random() * 1000));
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
     * 创建流式消息响应（委托给StreamMessageBuilder）
     */
    private StreamMessageResponseDto createStreamMessage(String content, String type, boolean isFinish, StreamingContext ctx) {
        return streamMessageBuilder.createStreamMessage(content, type, isFinish, ctx);
    }

    /**
     * 工具调用指令类
     */

    /**
     * 从AI响应中解析工具调用指令
     */
    private ToolCallInstruction parseToolCallFromResponse(String response, List<ToolDto> availableTools) {
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
                Pattern inputPattern = Pattern.compile(
                    "Action Input:\\s*(.+?)(?=\\n|$)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                Matcher inputMatcher = inputPattern.matcher(response);

                String parameters = inputMatcher.find() ? inputMatcher.group(1).trim() : "{}";
                // 使用匹配到的工具的实际名称，确保大小写一致
                return new ToolCallInstruction(matchedTool.getName(), matchedTool.getId(), matchedTool.getType(), parameters);
            }
        }

        return null;
    }

    /**
     * 根据工具名称解析工具ID
     */
    private Long resolveToolId(String toolName, StreamingContext ctx) {
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
     * 获取并验证模型配置（用于StreamMessageResponseDto）
     */
    private ModelConfigContext getAndValidateModelConfigsForStreamMessage(SysAgent agent, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        try {
            SysModelConfigVo mainModelConfig = getModelConfig(agent.getModel(), "主要模型");
            SysModelConfigVo enhanceModelConfig = getModelConfig(agent.getEnhanceModel(), "增强模型");

            // 验证主要模型配置
            if (mainModelConfig == null) {
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    "智能体主要模型配置不存在或无效，无法执行推理任务",
                    ctx.getCurrentChatId(),
                    generateMessageId(),
                    ctx.getAndIncrementMessageIndex()
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
                ctx.getCurrentChatId(),
                generateMessageId(),
                ctx.getAndIncrementMessageIndex()
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
                                                  boolean isFinalStep, int currentStep, StreamingContext ctx) {

        // ReAct模式第一次从思考开始
        String initialType = "thought";

        if (currentStep > 0) {
            initialType = "action";
        }

        // 重置缓冲区状态
        ctx.resetStreamBufferState(initialType);

        // 获取聊天服务
        IChatService chatService = aiService.getChatService(modelContext.getMainModel().getModelProvider());
        if (chatService == null) {
            sink.next(StreamMessageResponseDto.createErrorMessage(
                "无法获取聊天服务，提供商: " + modelContext.getMainModel().getModelProvider(),
                ctx.getCurrentChatId(),
                generateMessageId(),
                ctx.getAndIncrementMessageIndex()
            ));
            sink.complete();
            return;
        }

        // 构建聊天请求
        IChatRequest chatRequest = buildChatRequest(cotPrompt, modelContext.getMainModel());

        chatRequest.setStop(Collections.singletonList("Observation"));
        // 执行流式调用
        Flux<IChatResponse> modelStream = chatService.stream(chatRequest);
        processModelStreamForMessage(modelStream, modelContext, sink, fullResponseCallback, isFinalStep, initialType, ctx);
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
                                              boolean isFinalStep, String initialStreamType, StreamingContext ctx) {
        StringBuilder fullResponse = new StringBuilder();
        AtomicReference<IChatResponse> lastChunk = new AtomicReference<>();

        // 设置modelContext和sink到StreamingContext中
        ctx.setModelContext(modelContext);
        ctx.setCurrentSink(sink);

        modelStream
            .takeWhile(chunk -> !ctx.isShouldStopCurrentStream())  // 如果检测到应该停止，则停止处理流
            .doOnNext(chunk -> {
                lastChunk.set(chunk);
                handleStreamChunkForMessage(chunk, sink, fullResponse, initialStreamType, ctx);
            })
            .doOnComplete(() -> {
                // 检查是否因为增强回复而停止
                if (ctx.isShouldStopCurrentStream()) {
                    log.info("流式处理因增强回复而提前停止");
                    // 增强回复已经在processBufferedContent中启动，这里不需要额外处理
                    return;
                }

                // 在完成时累计token使用量
                accumulateTokenUsage(lastChunk.get(), ctx);

                if (isFinalStep) {
                    handleFinalStepCompleteForMessage(fullResponse.toString(), modelContext, sink, fullResponseCallback, ctx);
                } else {
                    handleIntermediateStepCompleteForMessage(fullResponse.toString(), sink, fullResponseCallback, ctx);
                }
            })
            .doOnError(error -> handleStreamErrorForMessage(error, sink, ctx))
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
                                             StringBuilder fullResponse, String initialStreamType, StreamingContext ctx) {
        if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
            String text = chunk.getResult().getOutput().getText();
            if (StringUtils.hasText(text)) {
                // 使用缓冲区处理内容
                processBufferedContent(text, sink, fullResponse, initialStreamType, ctx);
            }
        }
    }

    /**
     * 刷新缓冲区中的剩余内容
     */
    private void flushStreamBuffer(reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        StringBuilder buffer = ctx.getStreamBuffer();
        String currentType = ctx.getCurrentStreamType();

        // 如果缓冲区中还有内容，全部发送出去
        if (buffer.length() > 0) {
            String bufferContent = buffer.toString();
            sink.next(createStreamMessage(bufferContent, currentType, false, ctx));
            // 保存到思维过程中（除了answer类型的内容）
            if (!currentType.equals("answer")) {
                ctx.getThoughtProcess().append(bufferContent);
            }
            buffer.setLength(0);
        }
    }

    /**
     * 累计Token使用量
     */
    private void accumulateTokenUsage(IChatResponse response, StreamingContext ctx) {
        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            IChatResponse.Usage currentUsage = response.getMetadata().getUsage();
            IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();

            // 累加token使用量
            totalUsage.setPromptTokens(totalUsage.getPromptTokens() + currentUsage.getPromptTokens());
            totalUsage.setCompletionTokens(totalUsage.getCompletionTokens() + currentUsage.getCompletionTokens());
            totalUsage.setTotalTokens(totalUsage.getTotalTokens() + currentUsage.getTotalTokens());

            log.info("Token usage for this step - Prompt: {}, Completion: {}, Total: {}",
                currentUsage.getPromptTokens(),
                currentUsage.getCompletionTokens(),
                currentUsage.getTotalTokens());

            log.info("Cumulative token usage - Prompt: {}, Completion: {}, Total: {}",
                totalUsage.getPromptTokens(),
                totalUsage.getCompletionTokens(),
                totalUsage.getTotalTokens());
        }
    }

    /**
     * 获取Token使用量对象
     */
    private TokenUsageDto getTokenUsageObject(StreamingContext ctx) {
        IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();
        return TokenUsageDto.fromUsage(totalUsage);
    }

    /**
     * 报告总Token使用量
     */
    private void reportTotalTokenUsage(reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        // 获取token使用量对象
        TokenUsageDto tokenUsage = getTokenUsageObject(ctx);

        // 只有在有使用量时才报告
        if (tokenUsage.getTotalTokens() > 0) {
            // 将对象转换为JSON字符串
            String tokenUsageJson = JSONUtil.toJsonStr(tokenUsage);

            log.info("Total token usage for entire ReAct loop: {}", tokenUsageJson);
        }
    }

    /**
     * 处理缓冲区内容并检测关键词
     */
    private void processBufferedContent(String newText, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                        StringBuilder fullResponse, String initialStreamType, StreamingContext ctx) {
        StringBuilder buffer = ctx.getStreamBuffer();
        String currentType = ctx.getCurrentStreamType();
        boolean actionFound = ctx.isActionDetected();
        boolean finalAnswerFound = ctx.isFinalAnswerDetected();


        // 将新文本添加到缓冲区
        buffer.append(newText);
        fullResponse.append(newText);

        // 处理缓冲区内容
        String bufferContent = buffer.toString();
        StringBuilder toSend = new StringBuilder();

        while (bufferContent.length() > 0) {
            // 检查是否包含 "Final Answer"
            if (!finalAnswerFound) {
                int finalAnswerIndex = bufferContent.indexOf("Final Answer");
                if (finalAnswerIndex >= 0) {
                    // 发送 Final Answer 之前的内容
                    if (finalAnswerIndex > 0) {
                        String beforeFinal = bufferContent.substring(0, finalAnswerIndex);
                        sink.next(createStreamMessage(beforeFinal, currentType, false, ctx));
                        // 保存到思维过程中
                        ctx.getThoughtProcess().append(beforeFinal);
                    }

                    // 标记已找到 Final Answer
                    ctx.setFinalAnswerDetected(true);

                    // 检查是否需要增强回复
                    ModelConfigContext modelContext = ctx.getModelContext();
                    if (modelContext != null && modelContext.getEnhanceModel() != null
                        && !modelContext.getMainModel().getModelId().equals(modelContext.getEnhanceModel().getModelId())) {

                        // 需要增强回复，停止当前流
                        log.info("检测到Final Answer，准备使用增强模型进行回复增强");

                        // 保存当前已有的思维过程（包括"Final Answer"之前的内容）
                        String thoughtContent = ctx.getThoughtProcess().toString();

                        // 发送增强开始事件
                        sink.next(createStreamMessage("\n\n🎯 **增强回复...**\n", "enhancing", false, ctx));

                        // 设置标志停止当前流
                        ctx.setShouldStopCurrentStream(true);

                        // 清空缓冲区
                        buffer.setLength(0);

                        // 提取Final Answer后的内容作为原始答案
//                        String remainingContent = bufferContent.substring(finalAnswerIndex);
                        // 将剩余内容拼接到思维过程中，用于增强回复
                        String fullThoughtProcess = thoughtContent;

                        // 立即调用增强模型（需要异步处理）
                        executeEnhanceStreamingCall(fullThoughtProcess, modelContext, sink, ctx);

                        return;
                    }

                    // 不需要增强回复，按原逻辑处理
                    ctx.setCurrentStreamType("answer");
                    currentType = "answer";

                    // 发送 "Final Answer" 及之后的内容为 answer 类型
                    bufferContent = bufferContent.substring(finalAnswerIndex);

                    // 如果缓冲区还有内容，继续处理
                    if (bufferContent.length() > 20) {
                        // 发送超出20字符的部分
                        String toSendNow = bufferContent.substring(0, bufferContent.length() - 20);
                        sink.next(createStreamMessage(toSendNow, currentType, false, ctx));
                        bufferContent = bufferContent.substring(bufferContent.length() - 20);
                    }

                    buffer.setLength(0);
                    buffer.append(bufferContent);
                    return;
                }
            }

            // 检查是否包含 "Action"（仅在未找到 Final Answer 且未找到 Action 时）
            if (!finalAnswerFound && !actionFound) {
                int actionIndex = bufferContent.indexOf("Action");
                if (actionIndex >= 0) {
                    // 发送 Action 之前的内容为 thought 类型
                    if (actionIndex > 0) {
                        String beforeAction = bufferContent.substring(0, actionIndex);
                        sink.next(createStreamMessage(beforeAction, "thought", false, ctx));
                        // 保存到思维过程中
                        ctx.getThoughtProcess().append(beforeAction);
                    }

                    // 标记已找到 Action，切换类型为 action
                    ctx.setActionDetected(true);
                    ctx.setCurrentStreamType("action");
                    currentType = "action";

                    // 更新缓冲区，从 "Action" 开始
                    bufferContent = bufferContent.substring(actionIndex);
                }
            }

            // 处理缓冲区溢出（超过20字符）
            if (bufferContent.length() > 20) {
                // 发送超出20字符的部分
                String toSendNow = bufferContent.substring(0, bufferContent.length() - 20);
                sink.next(createStreamMessage(toSendNow, currentType, false, ctx));
                // 保存到思维过程中（除了answer类型的内容）
                if (!currentType.equals("answer")) {
                    ctx.getThoughtProcess().append(toSendNow);
                }
                bufferContent = bufferContent.substring(bufferContent.length() - 20);
            }

            // 更新缓冲区
            buffer.setLength(0);
            buffer.append(bufferContent);
            break;
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
    private void handleStreamErrorForMessage(Throwable error, reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        log.error("AI模型流式调用失败", error);

        // 发送缓冲区中剩余的内容
        flushStreamBuffer(sink, ctx);

        sink.next(StreamMessageResponseDto.createErrorMessage(
            "AI模型调用失败: " + error.getMessage(),
            ctx.getCurrentChatId(),
            generateMessageId(),
            ctx.getAndIncrementMessageIndex()
        ));
        sink.complete();
    }

    /**
     * 处理中间步骤的流式响应完成（用于StreamMessageResponseDto）
     */
    private void handleIntermediateStepCompleteForMessage(String stepResponse,
                                                          reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                          java.util.function.Consumer<String> fullResponseCallback, StreamingContext ctx) {
        log.info("ReAct中间步骤流式调用完成");

        // 发送缓冲区中剩余的内容
        flushStreamBuffer(sink, ctx);

        // 调用回调函数继续处理，但不关闭流
        fullResponseCallback.accept(stepResponse);
        // 注意：这里不调用 sink.complete()，保持流开放
    }

    /**
     * 处理最终步骤的流式响应完成（用于StreamMessageResponseDto）
     */
    private void handleFinalStepCompleteForMessage(String finalResponse, ModelConfigContext modelContext,
                                                   reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                                   java.util.function.Consumer<String> fullResponseCallback, StreamingContext ctx) {
        log.info("ReAct最终步骤流式调用完成");

        // 发送缓冲区中剩余的内容
        flushStreamBuffer(sink, ctx);

        fullResponseCallback.accept(finalResponse); // 调用回调函数

        // 如果配置了增强模型，且增强模型与主模型不同，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)
            && !modelContext.getMainModel().getModelId().equals(modelContext.getEnhanceModel().getModelId())) {
            // 发送增强开始事件
            sink.next(createStreamMessage("\n\n🎯 **增强回复...**\n", "enhancing", false, ctx));

            // 执行增强模型的流式调用
            executeEnhanceStreamingCall(finalResponse, modelContext, sink, ctx);
            return; // 增强调用会负责完成流
        }

        // 报告Token使用情况
        reportTotalTokenUsage(sink, ctx);

        // 创建带有token使用信息的完成消息
        StreamMessageResponseDto finishMessage = StreamMessageResponseDto.createFinishMessage(
            ctx.getCurrentChatId(),
            generateMessageId(),
            ctx.getAndIncrementMessageIndex()
        );

        // 将token使用信息添加到extraInfo
        TokenUsageDto tokenUsage = getTokenUsageObject(ctx);
        if (tokenUsage != null && tokenUsage.getTotalTokens() > 0) {
            finishMessage.getMessage().getExtraInfo().put("tokenUsage", tokenUsage);
        }

        // 发送完成消息
        sink.next(finishMessage);
        sink.complete();
    }

    /**
     * 执行增强模型的流式调用
     */
    private void executeEnhanceStreamingCall(String finalAnswer, ModelConfigContext modelContext,
                                             reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                             StreamingContext ctx) {
        try {
            // 获取增强模型的聊天服务
            IChatService enhanceChatService = aiService.getChatService(
                modelContext.getEnhanceModel().getModelProvider());

            if (enhanceChatService == null) {
                log.warn("无法获取增强模型的聊天服务: {}", modelContext.getEnhanceModel().getModelProvider());
                // 无法获取服务，直接完成
                completeWithTokenUsage(sink, ctx);
                return;
            }

            // 构建增强提示词，包含完整的推理过程
            String enhancePrompt = buildEnhancePrompt(finalAnswer, ctx);

            // 打印增强模型的提示词
            log.info("=== 增强模型提示词开始 ===");
            log.info("增强模型: {}", modelContext.getEnhanceModel().getModelId());
            log.info("提示词内容:\n{}", enhancePrompt);
            log.info("=== 增强模型提示词结束 ===");

            // 构建增强模型请求
            IChatRequest enhanceRequest = buildChatRequest(enhancePrompt, modelContext.getEnhanceModel());

            // 执行流式调用
            Flux<IChatResponse> enhanceStream = enhanceChatService.stream(enhanceRequest);

            // 处理增强模型的流式响应
            processEnhanceStream(enhanceStream, sink, ctx);

        } catch (Exception e) {
            log.error("增强模型调用失败", e);
            sink.next(createStreamMessage("\n⚠️ 增强失败，使用原始回复\n", "error", false, ctx));
            completeWithTokenUsage(sink, ctx);
        }
    }

    /**
     * 构建增强提示词
     */
    private String buildEnhancePrompt(String finalAnswer, StreamingContext ctx) {
        StringBuilder prompt = new StringBuilder();

        // 添加完整的提示词链（包含每一步的提示词和响应）
//        String promptChain = ctx.getPromptChain().toString();
//        if (StringUtils.hasText(promptChain)) {
//            prompt.append(promptChain);
//            prompt.append("\n\n");
//        }
//
//        // 添加对话历史（Thought-Action-Observation格式）
//        String history = ctx.getConversationHistory().toString();
//        if (StringUtils.hasText(history)) {
//            prompt.append(history);
//            prompt.append("\n\n");
//        }

        prompt.append(finalAnswer);

        return prompt.toString()
            .replace("Question", "用户问题")
            .replace("Thought", "思考")
            .replace("Observation", "工具执行的结果")
            .replace("Action Input", "工具入参")
            .replace("Action", "工具");
    }

    /**
     * 处理增强模型的流式响应
     */
    private void processEnhanceStream(Flux<IChatResponse> enhanceStream,
                                      reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                      StreamingContext ctx) {
        StringBuilder enhancedResponse = new StringBuilder();
        AtomicReference<IChatResponse> lastChunk = new AtomicReference<>();

        enhanceStream
            .doOnNext(chunk -> {
                lastChunk.set(chunk);
                if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
                    String text = chunk.getResult().getOutput().getText();
                    Object reasoningContent = chunk.getResult().getOutput().getReasoningContent();

                    // 如果有推理内容，先发送推理内容
                    if (reasoningContent != null) {
                        String reasoningText = reasoningContent.toString();
                        if (StringUtils.hasText(reasoningText)) {
                            enhancedResponse.append(reasoningText);
                            // 发送推理内容作为增强事件
                            sink.next(createStreamMessage(reasoningText, "enhanced_reason", false, ctx));
                        }
                    }

                    // 然后发送正常的文本内容
                    if (StringUtils.hasText(text)) {
                        enhancedResponse.append(text);
                        // 发送增强内容
                        sink.next(createStreamMessage(text, "enhanced", false, ctx));
                    }
                }
            })
            .doOnComplete(() -> {
                // 累计增强模型的token使用量
                accumulateTokenUsage(lastChunk.get(), ctx);

                // 保存增强后的回复到数据库
                if (enhancedResponse.length() > 0) {
                    log.info("增强模型优化完成，响应长度: {}", enhancedResponse.length());
                }

                // 完成整个流
                completeWithTokenUsage(sink, ctx);
            })
            .doOnError(error -> {
                log.error("增强模型流式调用失败", error);
                sink.next(createStreamMessage("\n⚠️ 增强失败: " + error.getMessage(), "error", false, ctx));
                completeWithTokenUsage(sink, ctx);
            })
            .subscribe();
    }

    /**
     * 完成流并报告Token使用情况
     */
    private void completeWithTokenUsage(reactor.core.publisher.FluxSink<StreamMessageResponseDto> sink,
                                        StreamingContext ctx) {
        // 报告Token使用情况
        reportTotalTokenUsage(sink, ctx);

        // 创建带有token使用信息的完成消息
        StreamMessageResponseDto finishMessage = StreamMessageResponseDto.createFinishMessage(
            ctx.getCurrentChatId(),
            generateMessageId(),
            ctx.getAndIncrementMessageIndex()
        );

        // 将token使用信息添加到extraInfo
        TokenUsageDto tokenUsage = getTokenUsageObject(ctx);
        if (tokenUsage != null && tokenUsage.getTotalTokens() > 0) {
            finishMessage.getMessage().getExtraInfo().put("tokenUsage", tokenUsage);
        }

        // 发送完成消息
        sink.next(finishMessage);
        sink.complete();
    }

    /**
     * 模型配置上下文类
     */

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
            StreamingContext ctx = null;
            try {
                SaTokenContext context = SaHolder.getContext();
                SaTokenContextMockUtil.setMockContext(() -> {
                    SaManager.setSaTokenContext(context);
                });
                // 创建StreamingContext
                ctx = new StreamingContext();

                // 创建简单的自由对话流式处理
                String chatIdStr = "chat_" + System.currentTimeMillis();
                String userMsgId = generateMessageId();
                ctx.setCurrentChatId(chatIdStr);
                ctx.setUserMessageId(userMsgId);

                // 发送开始消息
                sink.next(createStreamMessage("开始处理您的问题...", "thought", false, ctx));

                // 构建提示词
                String prompt = buildFreeChatPrompt(agent, userInput);

                // 发送回答
                String response = "这是一个模拟的回答。实际实现中应该调用AI模型。";
                sink.next(createStreamMessage(response, "answer", false, ctx));

                // 发送完成消息
                sink.next(StreamMessageResponseDto.createFinishMessage(chatIdStr, generateMessageId(), ctx.getAndIncrementMessageIndex()));
                sink.complete();

            } catch (Exception e) {
                log.error("自由对话流式执行失败", e);
                sink.next(StreamMessageResponseDto.createErrorMessage(
                    e.getMessage(),
                    ctx != null ? ctx.getCurrentChatId() : "unknown",
                    generateMessageId(),
                    ctx != null ? ctx.getAndIncrementMessageIndex() : 0
                ));
                sink.complete();
            }
        });
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

    /**
     * 流式处理上下文类，用于替代ThreadLocal
     */
}
