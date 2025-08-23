package org.dromara.system.controller.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/model/chat")
public class LlmModelChatController {
    @PostMapping(path = "/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chat(@RequestBody IChatRequest iChatRequest) {

        IChatService chatService = AiService.getChatService(iChatRequest.getCode());

        if (iChatRequest.getStream() != null && iChatRequest.getStream()) {
            // 流式响应：直接返回 Flux，Spring Boot 会自动处理为 SSE
            return chatService.stream(iChatRequest);
        } else {
            // 非流式响应：返回普通 JSON 对象
            // 确保设置为非流式
            iChatRequest.setStream(false);
            return chatService.stream(iChatRequest).blockFirst();
        }
    }
}
