package org.dromara.system.service;

import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.ChatResponseDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SysAgentChatService {

    /**
     * 处理流式对话完成
     *
     * @param chatRequest 聊天请求参数
     * @return 流式响应
     */
    Flux<StreamMessageResponseDto> completions(ChatRequestDto chatRequest);

}
