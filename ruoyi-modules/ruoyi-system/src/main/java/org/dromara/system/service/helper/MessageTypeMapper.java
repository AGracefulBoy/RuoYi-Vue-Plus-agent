package org.dromara.system.service.helper;

import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 消息类型映射器
 * 用于将消息类型映射为SSE事件类型
 *
 * @author system
 */
@Component
public class MessageTypeMapper {

    private static final Map<String, String> TYPE_TO_EVENT = Map.of(
        "thought", "thought",
        "action", "action",
        "enhanced_reason", "action",
        "enhancing", "action",
        "observation", "action",
        "answer", "answer",
        "enhanced", "answer",
        "error", "error",
        "finish", "complete"
    );

    /**
     * 将消息数据映射为SSE事件类型
     *
     * @param data 流式消息响应数据
     * @return SSE事件类型
     */
    public String mapToEventType(StreamMessageResponseDto data) {
        // 如果消息已完成，返回complete
        if (Boolean.TRUE.equals(data.getIsFinish())) {
            return "complete";
        }

        // 根据消息类型映射事件类型
        if (data.getMessage() != null && data.getMessage().getType() != null) {
            String messageType = data.getMessage().getType();

            // 特殊处理一些类型
            if ("enhanced_reason".equals(messageType) || "enhancing".equals(messageType)) {
                return "action";
            }
            if ("enhanced".equals(messageType)) {
                return "answer";
            }

            // 使用映射表
            return TYPE_TO_EVENT.getOrDefault(messageType, "message");
        }

        return "message";
    }
}
