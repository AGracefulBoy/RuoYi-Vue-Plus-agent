package org.dromara.system.service.helper;

import cn.hutool.core.util.IdUtil;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流式消息构建辅助类
 * 用于创建各种类型的流式消息
 *
 * @author system
 */
@Component
public class StreamMessageBuilder {

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return IdUtil.fastSimpleUUID();
    }

    /**
     * 创建流式消息（通用方法）
     */
    public StreamMessageResponseDto createStreamMessage(String content, String type, boolean isFinish, StreamingContext ctx) {
        String messageId = generateMessageId();

        switch (type) {
            case "thought":
                return createThoughtMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex());
            case "action":
                return createActionMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex(), null);
            case "observation":
                return createToolMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex(), null);
            case "answer":
                return createAnswerMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex(), isFinish);
            case "error":
                return createErrorMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getAndIncrementMessageIndex());
            case "enhanced_reason":
                return createEnhancingReasonMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex());
            case "enhancing":
                return createEnhancingMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex());
            case "enhanced":
                return createEnhancedMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex(), isFinish);
            default:
                return createAnswerMessage(content, ctx.getCurrentChatId(), messageId,
                    ctx.getUserMessageId(), ctx.getAndIncrementMessageIndex(), isFinish);
        }
    }

    /**
     * 创建思考类型的消息
     */
    public StreamMessageResponseDto createThoughtMessage(String content, String chatId, String messageId,
                                                        String replyId, Integer index) {
        return StreamMessageResponseDto.createThoughtMessage(content, chatId, messageId, replyId, index);
    }

    /**
     * 创建动作类型的消息
     */
    public StreamMessageResponseDto createActionMessage(String content, String chatId, String messageId,
                                                       String replyId, Integer index, Map<String, Object> toolInfo) {
        return StreamMessageResponseDto.createActionMessage(content, chatId, messageId, replyId, index, toolInfo);
    }

    /**
     * 创建答案类型的消息
     */
    public StreamMessageResponseDto createAnswerMessage(String content, String chatId, String messageId,
                                                       String replyId, Integer index, boolean isFinish) {
        return StreamMessageResponseDto.createAnswerMessage(content, chatId, messageId, replyId, index, isFinish);
    }

    /**
     * 创建工具响应类型的消息
     */
    public StreamMessageResponseDto createToolMessage(String content, String chatId, String messageId,
                                                     String replyId, Integer index, String toolName) {
        return StreamMessageResponseDto.createToolMessage(content, chatId, messageId, replyId, index, toolName);
    }

    /**
     * 创建完成消息
     */
    public StreamMessageResponseDto createFinishMessage(String chatId, String messageId, Integer index) {
        return StreamMessageResponseDto.createFinishMessage(chatId, messageId, index);
    }

    /**
     * 创建错误消息
     */
    public StreamMessageResponseDto createErrorMessage(String error, String chatId, String messageId, Integer index) {
        return StreamMessageResponseDto.createErrorMessage(error, chatId, messageId, index);
    }

    /**
     * 创建增强中的消息
     */
    public StreamMessageResponseDto createEnhancingReasonMessage(String content, String chatId, String messageId,
                                                           String replyId, Integer index) {
        return StreamMessageResponseDto.builder()
            .message(StreamMessageResponseDto.MessageData.builder()
                .role("assistant")
                .type("enhancing")
                .content(content)
                .contentType("text")
                .messageId(messageId)
                .replyId(replyId)
                .contentTime(System.currentTimeMillis())
                .build())
            .isFinish(false)
            .index(index)
            .chatId(chatId)
            .build();
    }

    /**
     * 创建增强中的消息
     */
    public StreamMessageResponseDto createEnhancingMessage(String content, String chatId, String messageId,
                                                          String replyId, Integer index) {
        return StreamMessageResponseDto.builder()
            .message(StreamMessageResponseDto.MessageData.builder()
                .role("assistant")
                .type("enhanced_reason")
                .content(content)
                .contentType("text")
                .messageId(messageId)
                .replyId(replyId)
                .contentTime(System.currentTimeMillis())
                .build())
            .isFinish(false)
            .index(index)
            .chatId(chatId)
            .build();
    }

    /**
     * 创建增强后的消息
     */
    public StreamMessageResponseDto createEnhancedMessage(String content, String chatId, String messageId,
                                                         String replyId, Integer index, boolean isFinish) {
        return StreamMessageResponseDto.builder()
            .message(StreamMessageResponseDto.MessageData.builder()
                .role("assistant")
                .type("enhanced")
                .content(content)
                .contentType("text")
                .messageId(messageId)
                .replyId(replyId)
                .contentTime(System.currentTimeMillis())
                .build())
            .isFinish(isFinish)
            .index(index)
            .chatId(chatId)
            .build();
    }
}
