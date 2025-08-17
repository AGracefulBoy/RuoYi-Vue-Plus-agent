package org.dromara.system.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.context.ModelConfigContext;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 增强辅助类
 * 负责处理回复增强，使用增强模型优化原始回复
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnhancementHelper {

    @Autowired
    private AiService aiService;

    @Autowired
    private StreamMessageBuilder streamMessageBuilder;

    @Autowired
    private MessagePersistenceHelper messagePersistenceHelper;

    @Autowired
    private TokenUsageHelper tokenUsageHelper;

    /**
     * 执行增强模型的流式调用
     *
     * @param finalAnswer  最终答案
     * @param agent        智能体
     * @param modelContext 模型配置上下文
     * @param sink         流式输出
     * @param ctx          流式上下文
     */
    public void executeEnhanceStreamingCall(String finalAnswer, SysAgent agent, ModelConfigContext modelContext,
                                            FluxSink<StreamMessageResponseDto> sink,
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
            String enhancePrompt = buildEnhancePrompt(finalAnswer, agent, ctx);

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
            sink.next(streamMessageBuilder.createStreamMessage("\n⚠️ 增强失败，使用原始回复\n", "error", false, ctx));
            completeWithTokenUsage(sink, ctx);
        }
    }

    /**
     * 构建增强提示词
     *
     * @param finalAnswer 最终答案
     * @param agent       智能体
     * @param ctx         流式上下文
     * @return 增强提示词
     */
    private String buildEnhancePrompt(String finalAnswer, SysAgent agent, StreamingContext ctx) {
        StringBuilder prompt = new StringBuilder();

        // 获取智能体人设，如果为空则使用默认值
        String agentPersonality = StringUtils.hasText(agent.getAgentPersonality()) ?
            agent.getAgentPersonality() : "智能助手";

        // 添加系统角色设定
        prompt.append(agentPersonality).append("。\n\n");

        // 添加原始推理过程和答案
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
     *
     * @param enhanceStream 增强流
     * @param sink          流式输出
     * @param ctx           流式上下文
     */
    private void processEnhanceStream(Flux<IChatResponse> enhanceStream,
                                      FluxSink<StreamMessageResponseDto> sink,
                                      StreamingContext ctx) {
        StringBuilder enhancedResponse = new StringBuilder();
        AtomicReference<IChatResponse> lastChunk = new AtomicReference<>();
        AtomicBoolean isCompleted = new AtomicBoolean(false);

        // 添加超时控制（60秒）和背压处理
        Disposable subscription = enhanceStream
            .timeout(Duration.ofSeconds(60))
            .onBackpressureBuffer(1000, // 缓冲区大小
                dropped -> log.warn("增强流背压：丢弃消息"),
                BufferOverflowStrategy.DROP_OLDEST) // 缓冲区满时丢弃最旧的
            .takeWhile(chunk -> !ctx.isShouldStopEnhanceStream()) // 添加流控制
            .doOnNext(chunk -> {
                // 检查 sink 状态
                if (sink.isCancelled()) {
                    log.warn("增强流：Sink 已取消，停止处理");
                    ctx.setShouldStopEnhanceStream(true);
                    return;
                }

                lastChunk.set(chunk);
                if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
                    String text = chunk.getResult().getOutput().getText();
                    Object reasoningContent = chunk.getResult().getOutput().getReasoningContent();

                    try {
                        // 如果有推理内容，先发送推理内容
                        if (reasoningContent != null) {
                            String reasoningText = reasoningContent.toString();
                            if (StringUtils.hasText(reasoningText) && !sink.isCancelled()) {
                                enhancedResponse.append(reasoningText);
                                // 发送推理内容作为增强事件
                                sink.next(streamMessageBuilder.createStreamMessage(reasoningText, "enhanced_reason", false, ctx));
                            }
                        }

                        // 然后发送正常的文本内容
                        if (StringUtils.hasText(text) && !sink.isCancelled()) {
                            enhancedResponse.append(text);
                            // 发送增强内容
                            sink.next(streamMessageBuilder.createStreamMessage(text, "enhanced", false, ctx));
                        }
                    } catch (Exception e) {
                        log.error("发送增强消息时出错", e);
                        // 不要在这里中断流，让它继续处理
                    }
                }
            })
            .doOnComplete(() -> {
                if (isCompleted.compareAndSet(false, true)) {
                    // 累计增强模型的token使用量
                    tokenUsageHelper.accumulateTokenUsage(lastChunk.get(), ctx);

                    // 保存增强后的回复到数据库
                    if (enhancedResponse.length() > 0) {
                        log.info("增强模型优化完成，响应长度: {}", enhancedResponse.length());
                        // 保存增强回复到数据库
                        messagePersistenceHelper.saveEnhancedReply(ctx.getChatId(), enhancedResponse.toString(), ctx, ctx.getTotalTokenUsage());
                    }

                    // 完成整个流
                    if (!sink.isCancelled()) {
                        completeWithTokenUsage(sink, ctx);
                    }
                }
            })
            .doOnError(error -> {
                if (isCompleted.compareAndSet(false, true)) {
                    if (error instanceof TimeoutException) {
                        log.error("增强模型流式调用超时", error);
                        if (!sink.isCancelled()) {
                            sink.next(streamMessageBuilder.createStreamMessage("\n⚠️ 增强超时，使用原始回复\n", "error", false, ctx));
                        }
                    } else {
                        log.error("增强模型流式调用失败", error);
                        if (!sink.isCancelled()) {
                            sink.next(streamMessageBuilder.createStreamMessage("\n⚠️ 增强失败: " + error.getMessage(), "error", false, ctx));
                        }
                    }

                    if (!sink.isCancelled()) {
                        completeWithTokenUsage(sink, ctx);
                    }
                }
            })
            .doFinally(signal -> {
                // 确保在任何情况下都清理资源
                log.debug("增强流结束，信号类型: {}", signal);
                ctx.setShouldStopEnhanceStream(false); // 重置标志
            })
            .subscribe(
                // onNext - 由 doOnNext 处理
                chunk -> {},
                // onError - 由 doOnError 处理
                error -> {},
                // onComplete - 由 doOnComplete 处理
                () -> {}
            );

        // 保存订阅对象到上下文，以便在需要时取消
        ctx.setEnhanceStreamSubscription(subscription);
    }

    /**
     * 使用增强模型优化回复（同步版本）
     *
     * @param originalResponse   原始响应
     * @param enhanceModelConfig 增强模型配置
     * @param agent              智能体
     * @return 增强后的响应
     */
    public String enhanceResponse(String originalResponse, SysModelConfigVo enhanceModelConfig, SysAgent agent) {
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

            // 调用增强模型（这里需要实际调用，暂时返回原始内容）
            // String enhancedResponse = enhanceChatService.chat(enhanceRequest);

            // 暂时返回原始内容
            return originalResponse;

        } catch (Exception e) {
            log.error("增强模型优化失败，返回原始回复", e);
            return originalResponse;
        }
    }

    /**
     * 构建聊天请求
     *
     * @param prompt      提示词
     * @param modelConfig 模型配置
     * @return 聊天请求
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
     * 完成流并报告Token使用情况
     *
     * @param sink 流式输出
     * @param ctx  流式上下文
     */
    private void completeWithTokenUsage(FluxSink<StreamMessageResponseDto> sink,
                                        StreamingContext ctx) {
        // 报告Token使用情况
        tokenUsageHelper.reportTotalTokenUsage(sink, ctx);

        // 创建带有token使用信息的完成消息
        StreamMessageResponseDto finishMessage = StreamMessageResponseDto.createFinishMessage(
            ctx.getCurrentChatId(),
            generateMessageId(),
            ctx.getAndIncrementMessageIndex()
        );

        // 将token使用信息添加到extraInfo
        var tokenUsage = tokenUsageHelper.getTokenUsageObject(ctx);
        if (tokenUsage != null && tokenUsage.getTotalTokens() > 0) {
            finishMessage.getMessage().getExtraInfo().put("tokenUsage", tokenUsage);
        }

        // 发送完成消息
        sink.next(finishMessage);
        sink.complete();
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return String.valueOf(System.currentTimeMillis() + (int) (Math.random() * 1000));
    }
}