package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 流式消息响应DTO
 * 用于SSE推送的结构化消息格式
 *
 * @author assistant
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StreamMessageResponseDto {

    /**
     * 消息对象
     */
    private MessageData message;

    /**
     * 是否结束
     */
    @Builder.Default
    private Boolean isFinish = false;

    /**
     * 消息索引
     */
    private Integer index;

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 消息数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageData {
        /**
         * 角色（assistant/user/system/tool）
         */
        @Builder.Default
        private String role = "assistant";

        /**
         * 消息类型（thought/action/answer）
         */
        private String type;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 内容类型
         */
        @Builder.Default
        private String contentType = "text";

        /**
         * 消息ID
         */
        private String messageId;

        /**
         * 回复的消息ID
         */
        private String replyId;

        /**
         * 会话段ID
         */
        private String sectionId;

        /**
         * 额外信息
         */
        @Builder.Default
        private Map<String, Object> extraInfo = new HashMap<>();

        /**
         * 内容时间戳
         */
        private Long contentTime;
    }

    /**
     * 创建思考类型的消息
     */
    public static StreamMessageResponseDto createThoughtMessage(String content, String chatId, String messageId, 
                                                                String replyId, Integer index) {
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("assistant")
                        .type("thought")
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
     * 创建动作类型的消息
     */
    public static StreamMessageResponseDto createActionMessage(String content, String chatId, String messageId, 
                                                               String replyId, Integer index, Map<String, Object> toolInfo) {
        Map<String, Object> extraInfo = new HashMap<>();
        if (toolInfo != null) {
            extraInfo.putAll(toolInfo);
        }
        
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("assistant")
                        .type("action")
                        .content(content)
                        .contentType("text")
                        .messageId(messageId)
                        .replyId(replyId)
                        .extraInfo(extraInfo)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(false)
                .index(index)
                .chatId(chatId)
                .build();
    }

    /**
     * 创建答案类型的消息
     */
    public static StreamMessageResponseDto createAnswerMessage(String content, String chatId, String messageId, 
                                                               String replyId, Integer index, boolean isFinish) {
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("assistant")
                        .type("answer")
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

    /**
     * 创建工具响应类型的消息
     */
    public static StreamMessageResponseDto createToolMessage(String content, String chatId, String messageId, 
                                                            String replyId, Integer index, String toolName) {
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("toolName", toolName);
        
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("tool")
                        .type("observation")
                        .content(content)
                        .contentType("text")
                        .messageId(messageId)
                        .replyId(replyId)
                        .extraInfo(extraInfo)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(false)
                .index(index)
                .chatId(chatId)
                .build();
    }

    /**
     * 创建完成消息
     */
    public static StreamMessageResponseDto createFinishMessage(String chatId, String messageId, Integer index) {
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("assistant")
                        .type("finish")
                        .content("")
                        .contentType("text")
                        .messageId(messageId)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(true)
                .index(index)
                .chatId(chatId)
                .build();
    }

    /**
     * 创建错误消息
     */
    public static StreamMessageResponseDto createErrorMessage(String error, String chatId, String messageId, Integer index) {
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("error", true);
        extraInfo.put("errorMessage", error);
        
        return StreamMessageResponseDto.builder()
                .message(MessageData.builder()
                        .role("assistant")
                        .type("error")
                        .content(error)
                        .contentType("text")
                        .messageId(messageId)
                        .extraInfo(extraInfo)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(true)
                .index(index)
                .chatId(chatId)
                .build();
    }
}