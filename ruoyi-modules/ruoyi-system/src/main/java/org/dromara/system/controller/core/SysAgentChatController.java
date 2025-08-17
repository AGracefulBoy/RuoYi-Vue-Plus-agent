package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.ratelimiter.annotation.RateLimiter;
import org.dromara.common.ratelimiter.enums.LimitType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.web.core.BaseController;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.DeleteDebugChatsDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.vo.SysAgentChatDetailVo;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
public class SysAgentChatController extends BaseController {

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

        // 在响应式流处理前保存线程上下文信息
        Long userId = LoginHelper.getUserId();
        String tenantId = LoginHelper.getTenantId();

        long startTime = System.currentTimeMillis();
        log.info("开始处理聊天请求 - 追踪ID: {}, 用户ID: {}, 租户ID: {}, 智能体ID: {}",
            chatRequest.getTraceId(), userId, tenantId, chatRequest.getAgentId());

        // 调用服务层处理流式对话
        Flux<StreamMessageResponseDto> responseFlux = sysAgentChatService.completions(chatRequest);

        // 使用计数器替代UUID生成以提升性能
        final AtomicLong eventIdCounter = new AtomicLong();
        final AtomicBoolean isCancelled = new AtomicBoolean(false);

        // 包装响应式流以确保上下文在整个链路中传递
        return SaTokenReactiveHelper.wrapFlux(responseFlux
            // 传递上下文信息到响应式流中
            .contextWrite(ctx -> ctx
                .put("userId", userId)
                .put("tenantId", tenantId)
                .put("traceId", chatRequest.getTraceId()))
            // 添加全局超时控制（5分钟）
            .timeout(Duration.ofMinutes(5))
            .map(data -> {
                // 使用MessageTypeMapper统一处理消息类型映射
                String eventType = messageTypeMapper.mapToEventType(data);

                // 使用递增ID替代UUID
                String eventId = chatRequest.getTraceId() + "_" + eventIdCounter.incrementAndGet();

                return ServerSentEvent.<StreamMessageResponseDto>builder()
                    .id(eventId)
                    .event(eventType)
                    .data(data)
                    .build();
            })
            // 客户端断开连接时的处理
            .doOnCancel(() -> {
                isCancelled.set(true);
                long duration = System.currentTimeMillis() - startTime;
                log.warn("SSE连接被客户端断开 - 追踪ID: {}, 耗时: {}ms, 用户ID: {}",
                    chatRequest.getTraceId(), duration, userId);
                // 通知服务层取消正在进行的操作
                sysAgentChatService.cancelOngoingOperations(chatRequest.getTraceId());
//                // 更新会话状态为已取消
//                if (chatRequest.getChatId() != null) {
//                    sysAgentChatService.updateChatStatus(chatRequest.getChatId(), "cancelled");
//                }
            })
            .doOnComplete(() -> {
                if (!isCancelled.get()) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("聊天请求处理完成 - 追踪ID: {}, 耗时: {}ms", chatRequest.getTraceId(), duration);
                }
            })
            .doOnError(error -> {
                long duration = System.currentTimeMillis() - startTime;
                // 区分不同类型的错误
                if (error instanceof TimeoutException) {
                    log.error("聊天请求超时 - 追踪ID: {}, 耗时: {}ms",
                        chatRequest.getTraceId(), duration);
                } else {
                    log.error("聊天请求处理失败 - 追踪ID: {}, 耗时: {}ms, 错误: {}",
                        chatRequest.getTraceId(), duration, error.getMessage(), error);
                }
            })
            // 清理资源
            .doFinally(signal -> {
                log.debug("SSE流结束 - 追踪ID: {}, 信号类型: {}", chatRequest.getTraceId(), signal);
                // 清理可能的资源
                sysAgentChatService.cleanupResources(chatRequest.getTraceId());
            })
            .onErrorResume(error -> {
                String errorId = chatRequest.getTraceId() + "_error_" + System.currentTimeMillis();
                String errorMessage;
                String errorType;

                // 根据异常类型构建友好的错误消息
                if (error instanceof TimeoutException) {
                    errorMessage = "处理超时，请稍后重试";
                    errorType = "timeout";
                } else if (error.getMessage() != null && error.getMessage().contains("模型")) {
                    errorMessage = "AI模型服务暂时不可用，请稍后重试";
                    errorType = "model_error";
                } else if (error.getMessage() != null && error.getMessage().contains("工具")) {
                    errorMessage = "工具执行失败，请检查参数后重试";
                    errorType = "tool_error";
                } else {
                    errorMessage = "服务处理异常，请稍后重试";
                    errorType = "unknown";
                }

                StreamMessageResponseDto errorResponse = StreamMessageResponseDto.createErrorMessage(
                    errorMessage,
                    errorId,
                    String.valueOf(System.currentTimeMillis()),
                    0
                );
                errorResponse.setErrorType(errorType);
                errorResponse.setCanRetry(true);

                return Flux.just(ServerSentEvent.<StreamMessageResponseDto>builder()
                    .id(errorId)
                    .event("error")
                    .data(errorResponse)
                    .build());
            }));
    }

    /**
     * 根据智能体ID分页查询会话记录
     *
     * @param agentId 智能体ID
     * @param chatModel 对话模式
     * @param pageQuery 分页参数
     * @return 会话记录列表
     */
    @GetMapping("/agent/list")
    @SaCheckPermission("system:agent:list")
    public TableDataInfo<SysAgentChatDetailVo> listByAgent(@RequestParam Long agentId,
                                                           @RequestParam(required = false) String chatModel,
                                                           PageQuery pageQuery) {
        return sysAgentChatService.queryPageListByAgent(agentId, chatModel, pageQuery);
    }

    /**
     * 删除智能体的debug模式对话记录
     *
     * @param dto 删除请求参数
     * @return 删除结果
     */
    @PostMapping("/deleteDebugChats")
    @SaCheckPermission("system:agent:remove")
    @Log(title = "删除debug对话记录", businessType = BusinessType.DELETE)
    public R<Void> deleteDebugChats(@Valid @RequestBody DeleteDebugChatsDto dto) {
        return toAjax(sysAgentChatService.deleteDebugChatsByAgentId(dto.getAgentId()));
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
