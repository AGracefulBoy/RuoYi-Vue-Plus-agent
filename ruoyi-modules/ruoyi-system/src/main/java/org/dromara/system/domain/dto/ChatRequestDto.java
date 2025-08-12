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
     * 会话ID（可选，继续会话时必填；若为空则创建新会话）
     */
    private String conversationId;

    /**
     * 分组ID（可选）
     */
    private Long groupId;

    /**
     * 对话模式（debug表示调试，chat表示正常对话）
     */
    private String chatModel;

    /**
     * 追踪ID（用于调用链追踪）
     */
    private String traceId;
}
