package org.dromara.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 删除debug聊天记录请求DTO
 *
 * @author system
 */
@Data
public class DeleteDebugChatsDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 智能体ID
     */
    @NotNull(message = "智能体ID不能为空")
    private Long agentId;
}