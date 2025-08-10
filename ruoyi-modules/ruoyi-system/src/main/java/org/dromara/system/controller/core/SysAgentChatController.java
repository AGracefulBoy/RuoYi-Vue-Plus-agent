package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;

import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.ChatResponseDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.StreamingChatResponseDto;
import org.dromara.system.domain.vo.ChatHistoryVo;
import org.dromara.system.domain.vo.ChatSessionVo;
import org.dromara.system.service.ChatContextService;
import org.dromara.system.service.SysAgentChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private ChatContextService chatContextService;

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

        // 调用服务层处理流式对话
        return sysAgentChatService.completions(chatRequest)
            .map(data -> {
                // 根据消息类型决定事件类型
                String eventType = "message";
                if (data.getMessage() != null) {
                    String messageType = data.getMessage().getType();
                    if ("thought".equals(messageType)) {
                        eventType = "thought";
                    } else if ("action".equals(messageType) || "enhanced_reason".equals(messageType) || "enhancing".equals(messageType)) {
                        eventType = "action";
                    } else if ("observation".equals(messageType)) {
                        eventType = "observation";
                    } else if ("answer".equals(messageType) || "enhanced".equals(messageType)) {
                        eventType = "answer";
                    } else if ("error".equals(messageType)) {
                        eventType = "error";
                    } else if ("finish".equals(messageType)) {
                        eventType = "complete";
                    }
                }

                if (data.getIsFinish() != null && data.getIsFinish()) {
                    eventType = "complete";
                }

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
                .build()));
    }

    /**
     * 创建新会话
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @PostMapping("/sessions")
    public R<ChatSessionVo> createSession(@RequestParam @NotNull(message = "智能体ID不能为空") Long agentId,
                                          @RequestParam(required = false) String title) {
        Long userId = LoginHelper.getUserId();
        SysAgentChat chat = chatContextService.createChat(agentId, userId, title);

        ChatSessionVo vo = new ChatSessionVo();
        vo.setChatId(chat.getChatId());
        vo.setChatUuid(chat.getChatUuid());
        vo.setAgentId(chat.getAgentId());
        vo.setTitle(chat.getChatTitle());
        vo.setStatus(chat.getStatus());
        vo.setCreatedTime(chat.getCreateTime());

        return R.ok(vo);
    }

    /**
     * 获取会话列表
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping("/sessions")
    public R<List<ChatSessionVo>> getSessions(@RequestParam(required = false) Long agentId,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "20") int limit) {
        Long userId = LoginHelper.getUserId();
        List<SysAgentChat> chats = chatContextService.getUserChats(userId, agentId, status, limit);

        List<ChatSessionVo> vos = chats.stream().map(chat -> {
            ChatSessionVo vo = new ChatSessionVo();
            vo.setChatId(chat.getChatId());
            vo.setChatUuid(chat.getChatUuid());
            vo.setAgentId(chat.getAgentId());
            vo.setTitle(chat.getChatTitle());
            vo.setStatus(chat.getStatus());
            vo.setMessageCount(chat.getMessageCount());
            vo.setLastActiveTime(chat.getLastActiveTime());
            vo.setCreatedTime(chat.getCreateTime());
            return vo;
        }).toList();

        return R.ok(vos);
    }

    /**
     * 获取会话历史
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping("/sessions/{chatUuid}/history")
    public R<ChatHistoryVo> getChatHistory(@PathVariable @NotBlank(message = "会话UUID不能为空") String chatUuid,
                                           @RequestParam(defaultValue = "50") int limit) {
        SysAgentChat chat = chatContextService.getChatByUuid(chatUuid);
        if (chat == null) {
            return R.fail("会话不存在");
        }

        // 验证权限
        if (!chat.getUserId().equals(LoginHelper.getUserId())) {
            return R.fail("无权访问该会话");
        }

        ChatHistoryVo historyVo = new ChatHistoryVo();
        historyVo.setChatId(chat.getChatId());
        historyVo.setChatUuid(chat.getChatUuid());
        historyVo.setTitle(chat.getChatTitle());
        historyVo.setMessages(chatContextService.getChatHistory(chat.getChatId(), limit, false));
        historyVo.setTokenUsage(new ChatHistoryVo.TokenUsage(
            chat.getInputTokens(),
            chat.getOutputTokens(),
            chat.getTotalTokens()
        ));

        return R.ok(historyVo);
    }

    /**
     * 归档会话
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @PutMapping("/sessions/{chatUuid}/archive")
    public R<Void> archiveSession(@PathVariable @NotBlank(message = "会话UUID不能为空") String chatUuid) {
        SysAgentChat chat = chatContextService.getChatByUuid(chatUuid);
        if (chat == null) {
            return R.fail("会话不存在");
        }

        // 验证权限
        if (!chat.getUserId().equals(LoginHelper.getUserId())) {
            return R.fail("无权操作该会话");
        }

        chatContextService.archiveChat(chat.getChatId());
        return R.ok();
    }

    /**
     * 删除会话
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @DeleteMapping("/sessions/{chatUuid}")
    public R<Void> deleteSession(@PathVariable @NotBlank(message = "会话UUID不能为空") String chatUuid) {
        SysAgentChat chat = chatContextService.getChatByUuid(chatUuid);
        if (chat == null) {
            return R.fail("会话不存在");
        }

        // 验证权限
        if (!chat.getUserId().equals(LoginHelper.getUserId())) {
            return R.fail("无权操作该会话");
        }

        chatContextService.deleteChat(chat.getChatId());
        return R.ok();
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
