package org.dromara.system.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.context.ModelConfigContext;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 流式处理辅助类
 * 负责处理模型的流式响应，包括缓冲区管理、内容解析和流控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamProcessingHelper {

    @Autowired
    private StreamMessageBuilder streamMessageBuilder;

    @Autowired
    private MessagePersistenceHelper messagePersistenceHelper;

    @Autowired
    private EnhancementHelper enhancementHelper;

    /**
     * 处理模型流式响应
     */
    public void processModelStream(Flux<IChatResponse> modelStream, ModelConfigContext modelContext,
                                   FluxSink<String> sink,
                                   Consumer<String> fullResponseCallback,
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
    public void processModelStreamForMessage(Flux<IChatResponse> modelStream, ModelConfigContext modelContext,
                                             FluxSink<StreamMessageResponseDto> sink,
                                             Consumer<String> fullResponseCallback,
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
    private void handleStreamChunk(IChatResponse chunk, FluxSink<String> sink,
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
    private void handleStreamChunkForMessage(IChatResponse chunk, FluxSink<StreamMessageResponseDto> sink,
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
     * 处理缓冲区内容并检测关键词
     */
    public void processBufferedContent(String newText, FluxSink<StreamMessageResponseDto> sink,
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

        while (bufferContent.length() > 0) {
            // 检查是否包含 "Final Answer"
            if (!finalAnswerFound) {
                int finalAnswerIndex = bufferContent.indexOf("Final Answer");
                if (finalAnswerIndex >= 0) {
                    // 发送 Final Answer 之前的内容
                    if (finalAnswerIndex > 0) {
                        String beforeFinal = bufferContent.substring(0, finalAnswerIndex);
                        sink.next(streamMessageBuilder.createStreamMessage(beforeFinal, currentType, false, ctx));
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

                        // 设置标志停止当前流
                        ctx.setShouldStopCurrentStream(true);

                        // 清空缓冲区
                        buffer.setLength(0);

                        // 立即调用增强模型
                        enhancementHelper.executeEnhanceStreamingCall(thoughtContent, (SysAgent) ctx.getAgent(), modelContext, sink, ctx);

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
                        sink.next(streamMessageBuilder.createStreamMessage(toSendNow, currentType, false, ctx));
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
                        sink.next(streamMessageBuilder.createStreamMessage(beforeAction, "thought", false, ctx));
                        // 保存到思维过程中
                        ctx.getThoughtProcess().append(beforeAction);
                    }

                    // 当出现Action时，保存之前的thought内容到数据库
                    if (ctx.getChatId() != null && ctx.getThoughtProcess().length() > 0) {
                        messagePersistenceHelper.saveThoughtMessage(ctx.getChatId(), ctx.getThoughtProcess().toString(), ctx);
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
                sink.next(streamMessageBuilder.createStreamMessage(toSendNow, currentType, false, ctx));
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
     * 刷新缓冲区中的剩余内容
     */
    public void flushStreamBuffer(FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        StringBuilder buffer = ctx.getStreamBuffer();
        String currentType = ctx.getCurrentStreamType();

        // 如果缓冲区中还有内容，全部发送出去
        if (buffer.length() > 0) {
            String bufferContent = buffer.toString();
            sink.next(streamMessageBuilder.createStreamMessage(bufferContent, currentType, false, ctx));
            // 保存到思维过程中（除了answer类型的内容）
            if (!currentType.equals("answer")) {
                ctx.getThoughtProcess().append(bufferContent);
            }
            buffer.setLength(0);
        }
    }

    /**
     * 处理中间步骤的流式响应完成
     */
    private void handleIntermediateStepComplete(String stepResponse,
                                                FluxSink<String> sink,
                                                Consumer<String> fullResponseCallback) {
        log.info("ReAct中间步骤流式调用完成");
        // 调用回调函数继续处理，但不关闭流
        fullResponseCallback.accept(stepResponse);
        // 注意：这里不调用 sink.complete()，保持流开放
    }

    /**
     * 处理最终步骤的流式响应完成
     */
    private void handleFinalStepComplete(String finalResponse, ModelConfigContext modelContext,
                                         FluxSink<String> sink,
                                         Consumer<String> fullResponseCallback) {
        log.info("ReAct最终步骤流式调用完成");
        fullResponseCallback.accept(finalResponse); // 调用回调函数

        // 如果配置了增强模型，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)) {
            try {
                // 明确告知客户端还有增强内容
                sink.next("\n\n[ENHANCING] 正在优化回复...\n");
                String enhancedResponse = enhancementHelper.enhanceResponse(finalResponse, modelContext.getEnhanceModel(), null);

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
    private void handleStreamError(Throwable error, FluxSink<String> sink) {
        log.error("AI模型流式调用失败", error);
        sink.error(new RuntimeException("AI模型调用失败: " + error.getMessage()));
    }

    /**
     * 处理流式响应错误（用于StreamMessageResponseDto）
     */
    private void handleStreamErrorForMessage(Throwable error, FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
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
                                                          FluxSink<StreamMessageResponseDto> sink,
                                                          Consumer<String> fullResponseCallback, StreamingContext ctx) {
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
                                                   FluxSink<StreamMessageResponseDto> sink,
                                                   Consumer<String> fullResponseCallback, StreamingContext ctx) {
        log.info("ReAct最终步骤流式调用完成");

        // 发送缓冲区中剩余的内容
        flushStreamBuffer(sink, ctx);

        fullResponseCallback.accept(finalResponse); // 调用回调函数

        // 如果配置了增强模型，且增强模型与主模型不同，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)
            && !modelContext.getMainModel().getModelId().equals(modelContext.getEnhanceModel().getModelId())) {
            // 发送增强开始事件
            sink.next(streamMessageBuilder.createStreamMessage("\n\n🎯 **增强回复...**\n", "enhancing", false, ctx));

            // 执行增强模型的流式调用
            enhancementHelper.executeEnhanceStreamingCall(finalResponse, (SysAgent) ctx.getAgent(), modelContext, sink, ctx);
            return; // 增强调用会负责完成流
        }

        // 保存最终答案到数据库
        messagePersistenceHelper.saveFinalAnswer(ctx.getChatId(), finalResponse, ctx, ctx.getTotalTokenUsage());

        // 报告Token使用情况并完成流
        completeWithTokenUsage(sink, ctx);
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
     * 完成流并报告Token使用情况
     */
    private void completeWithTokenUsage(FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        // 创建带有token使用信息的完成消息
        StreamMessageResponseDto finishMessage = StreamMessageResponseDto.createFinishMessage(
            ctx.getCurrentChatId(),
            generateMessageId(),
            ctx.getAndIncrementMessageIndex()
        );

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