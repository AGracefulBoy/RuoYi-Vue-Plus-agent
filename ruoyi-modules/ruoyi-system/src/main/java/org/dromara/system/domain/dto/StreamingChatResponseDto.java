package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * 流式聊天响应DTO（用于SSE推送）
 *
 * @author zhoudashuai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StreamingChatResponseDto {

    /**
     * 事件ID
     */
    private String eventId;

    /**
     * 事件类型
     */
    private EventType eventType;

    /**
     * 事件数据
     */
    private Object data;

    /**
     * 是否结束
     */
    private Boolean finished;

    /**
     * 时间戳
     */
    private Date timestamp;

    /**
     * 事件类型枚举
     */
    public enum EventType {
        /**
         * 连接建立
         */
        CONNECTION_ESTABLISHED,

        /**
         * 开始处理
         */
        PROCESSING_START,

        /**
         * 思维步骤
         */
        THOUGHT_STEP,

        /**
         * 工具调用开始
         */
        TOOL_CALL_START,

        /**
         * 工具调用结果
         */
        TOOL_CALL_RESULT,

        /**
         * 内容块（流式文本）
         */
        CONTENT_CHUNK,

        /**
         * 最终答案
         */
        FINAL_ANSWER,

        /**
         * Token使用更新
         */
        TOKEN_USAGE_UPDATE,

        /**
         * 错误
         */
        ERROR,

        /**
         * 处理完成
         */
        PROCESSING_COMPLETE,

        /**
         * 心跳
         */
        HEARTBEAT
    }

    /**
     * 思维步骤数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThoughtStepData {
        private String stepType;
        private String content;
        private Integer stepIndex;
        private Long duration;
    }

    /**
     * 工具调用数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallData {
        private String toolCallId;
        private String toolName;
        private String status;
        private Object parameters;
        private Object result;
        private String error;
    }

    /**
     * 内容块数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentChunkData {
        private String content;
        private Integer chunkIndex;
        private Boolean isComplete;
    }

    /**
     * Token使用数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsageData {
        private Integer inputTokens;
        private Integer outputTokens;
        private Integer totalTokens;
        private Double estimatedCost;
    }

    /**
     * 错误数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        private String errorCode;
        private String errorMessage;
        private String errorType;
        private Object errorDetails;
    }

    /**
     * 创建思维步骤事件
     */
    public static StreamingChatResponseDto createThoughtStepEvent(String stepType, String content, Integer stepIndex) {
        return StreamingChatResponseDto.builder()
                .eventId(generateEventId())
                .eventType(EventType.THOUGHT_STEP)
                .data(ThoughtStepData.builder()
                        .stepType(stepType)
                        .content(content)
                        .stepIndex(stepIndex)
                        .build())
                .finished(false)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建内容块事件
     */
    public static StreamingChatResponseDto createContentChunkEvent(String content, Integer chunkIndex) {
        return StreamingChatResponseDto.builder()
                .eventId(generateEventId())
                .eventType(EventType.CONTENT_CHUNK)
                .data(ContentChunkData.builder()
                        .content(content)
                        .chunkIndex(chunkIndex)
                        .isComplete(false)
                        .build())
                .finished(false)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建错误事件
     */
    public static StreamingChatResponseDto createErrorEvent(String errorCode, String errorMessage) {
        return StreamingChatResponseDto.builder()
                .eventId(generateEventId())
                .eventType(EventType.ERROR)
                .data(ErrorData.builder()
                        .errorCode(errorCode)
                        .errorMessage(errorMessage)
                        .errorType("PROCESSING_ERROR")
                        .build())
                .finished(true)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建完成事件
     */
    public static StreamingChatResponseDto createCompleteEvent() {
        return StreamingChatResponseDto.builder()
                .eventId(generateEventId())
                .eventType(EventType.PROCESSING_COMPLETE)
                .finished(true)
                .timestamp(new Date())
                .build();
    }

    /**
     * 生成事件ID
     */
    private static String generateEventId() {
        return "evt_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
}