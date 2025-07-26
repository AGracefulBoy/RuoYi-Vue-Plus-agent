package org.dromara.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


import javax.validation.constraints.NotNull;

/**
 * 聊天请求DTO
 *
 * @author zhoudashuai
 */
@Data
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
    private String chatId;

    /**
     * 是否流式响应
     */
    private Boolean stream = true;
}
