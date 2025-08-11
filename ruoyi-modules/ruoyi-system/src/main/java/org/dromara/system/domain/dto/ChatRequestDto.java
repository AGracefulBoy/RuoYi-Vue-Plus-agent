package org.dromara.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 聊天请求DTO
 *
 * @author zhoudashuai
 */
@Data
@Accessors(chain = true)
public class ChatRequestDto {

    /**
     * 智能体ID
     */
    @NotNull(message = "智能体ID不能为空")
    private Long agentId;

    /**
     * 用户问题
     */
    @NotBlank(message = "用户问题不能为空")
    private String message;

    /**
     * 聊天ID（可选，用于多轮对话）
     */
    private String chatUuid;

    /**
     * 对话模式，debug 代表提示，chat 表示正常对话
     */
    private String chatModel;

    /**
     * 追踪ID（用于调用链追踪）
     */
    private String traceId;
}
