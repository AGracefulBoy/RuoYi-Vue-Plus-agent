package org.dromara.system.service;

import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.ChatResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SysAgentChatService {

    /**
     * 处理流式对话完成
     * 
     * @param chatRequest 聊天请求参数
     * @return 流式响应
     */
    Flux<String> completions(ChatRequestDto chatRequest);

    /**
     * 处理同步对话完成
     * 
     * @param chatRequest 聊天请求参数
     * @return 完整响应
     */
    Mono<ChatResponseDto> completionsSync(ChatRequestDto chatRequest);
}
