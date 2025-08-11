package org.dromara.system.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.springframework.stereotype.Component;

/**
 * 消息类型映射器
 * 用于将消息类型映射为SSE事件类型
 * 使用switch表达式优化性能
 *
 * @author system
 */
@Slf4j
@Component
public class MessageTypeMapper {

    /**
     * 将消息数据映射为SSE事件类型
     * 使用switch表达式提升性能
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
            
            // 使用switch表达式代替Map查找，提升性能
            String eventType = switch (messageType) {
                case "thought" -> "thought";
                case "action", "enhanced_reason", "enhancing", "observation" -> "action";
                case "answer", "enhanced" -> "answer";
                case "error" -> "error";
                case "finish" -> "complete";
                default -> {
                    log.debug("未知消息类型: {}, 使用默认事件类型: message", messageType);
                    yield "message";
                }
            };
            
            return eventType;
        }

        return "message";
    }
}
