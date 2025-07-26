package org.dromara.system.service;

import org.dromara.system.domain.dto.ChatRequestDto;
import reactor.core.publisher.Flux;

public interface SysAgentChatService {

    /**
     * 处理流式对话完成
     * 
     * @param chatRequest 聊天请求参数
     * @return 流式响应
     */
    Flux<String> completions(ChatRequestDto chatRequest);
}
