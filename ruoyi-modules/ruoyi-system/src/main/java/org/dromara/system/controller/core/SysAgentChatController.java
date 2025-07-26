package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.service.SysAgentChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;

/**
 * 核心对话接口
 *
 * @author zhoudashuai
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/chat/v1")
public class SysAgentChatController {

    @Autowired
    private SysAgentChatService sysAgentChatService;

    /**
     * 流式生成接口 - 支持实时获取生成过程的分块响应
     */
    @SaCheckPermission("system:agent:list")
    @PostMapping(path = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<String> stream(@Valid @RequestBody ChatRequestDto chatRequest) {
        // 调用服务层处理流式对话
        return sysAgentChatService.completions(chatRequest);
    }
}
