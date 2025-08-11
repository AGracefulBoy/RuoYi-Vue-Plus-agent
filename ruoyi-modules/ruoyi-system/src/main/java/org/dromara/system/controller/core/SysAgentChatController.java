package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.service.SysAgentChatService;
import org.dromara.system.service.helper.MessageTypeMapper;
import org.dromara.system.service.helper.SaTokenReactiveHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.validation.Valid;
import java.time.Duration;

/**
 * 核心对话接口
 *
 * @author zhoudashuai
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/chat/v1")
public class SysAgentChatController {

    @Autowired
    private SysAgentChatService sysAgentChatService;

    @Autowired
    private MessageTypeMapper messageTypeMapper;

    /**
     * 流式生成接口 - 支持实时获取生成过程的分块响应
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @RateLimiter(key = "chat:completions", time = 60, count = 10, limitType = LimitType.IP)
    @PostMapping(path = "/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Flux<ServerSentEvent<StreamMessageResponseDto>> stream(@Valid @RequestBody ChatRequestDto chatRequest) {
        // 生成追踪ID
        if (StrUtil.isBlank(chatRequest.getTraceId())) {
            chatRequest.setTraceId(IdUtil.fastSimpleUUID());
        }

        log.info("开始处理聊天请求，追踪ID: {}, 用户ID: {}, 智能体ID: {}",
            chatRequest.getTraceId(), LoginHelper.getUserId(), chatRequest.getAgentId());

        // 调用服务层处理流式对话，并使用 SaTokenReactiveHelper 包装整个响应式链
        Flux<StreamMessageResponseDto> responseFlux = sysAgentChatService.completions(chatRequest);

        // 包装响应式流以确保上下文在整个链路中传递
        return SaTokenReactiveHelper.wrapFlux(responseFlux
            .map(data -> {
                // 使用MessageTypeMapper统一处理消息类型映射
                String eventType = messageTypeMapper.mapToEventType(data);

                return ServerSentEvent.<StreamMessageResponseDto>builder()
                    .id(IdUtil.fastSimpleUUID())
                    .event(eventType)
                    .data(data)
                    .build();
            })
            .doOnError(error -> log.error("聊天请求处理失败，追踪ID: {}", chatRequest.getTraceId(), error))
            .onErrorResume(error -> Flux.just(ServerSentEvent.<StreamMessageResponseDto>builder()
                .event("error")
                .data(StreamMessageResponseDto.createErrorMessage(
                    error.getMessage(),
                    "error_" + System.currentTimeMillis(),
                    String.valueOf(System.currentTimeMillis()),
                    0
                ))
                .build())));
    }

    /**
     * 健康检查接口（用于SSE连接测试）
     */
    @GetMapping(value = "/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> health() {
        return Flux.interval(Duration.ofSeconds(5))
            .take(3)
            .map(seq -> ServerSentEvent.<String>builder()
                .id(String.valueOf(seq))
                .event("heartbeat")
                .data("{\"status\": \"healthy\", \"timestamp\": " + System.currentTimeMillis() + "}")
                .build());
    }
}
