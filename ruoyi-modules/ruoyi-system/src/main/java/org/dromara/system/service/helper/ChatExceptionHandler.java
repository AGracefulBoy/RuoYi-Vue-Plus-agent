package org.dromara.system.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.mapper.SysAgentChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 聊天异常处理器
 * 统一管理异常状态、熔断器、重试策略等
 *
 * @author system
 */
@Slf4j
@Component
public class ChatExceptionHandler {

    @Autowired(required = false)
    private SysAgentChatMessageMapper chatMessageMapper;

    /**
     * 异常计数器 - 追踪每个会话的错误次数
     */
    private final Map<String, AtomicInteger> errorCounters = new ConcurrentHashMap<>();

    /**
     * 熔断器状态
     */
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    /**
     * 最后异常时间记录
     */
    private final Map<String, Instant> lastErrorTime = new ConcurrentHashMap<>();

    /**
     * 异常上下文缓存
     */
    private final Map<String, ExceptionContext> exceptionContexts = new ConcurrentHashMap<>();

    /**
     * 熔断器配置
     */
    private static final int ERROR_THRESHOLD = 5; // 错误阈值
    private static final Duration CIRCUIT_BREAK_DURATION = Duration.ofMinutes(5); // 熔断持续时间
    private static final Duration ERROR_WINDOW = Duration.ofMinutes(1); // 错误统计窗口

    /**
     * 处理异常
     *
     * @param chatId 会话ID
     * @param e      异常对象
     * @param ctx    流式上下文
     */
    public void handleException(String chatId, Exception e, StreamingContext ctx) {
        try {
            // 记录异常
            logException(chatId, e);
            
            // 更新错误计数
            int errorCount = incrementErrorCount(chatId);
            
            // 检查熔断
            if (shouldCircuitBreak(chatId)) {
                throw new CircuitBreakerOpenException(
                    "服务暂时不可用，已触发熔断保护。请" + 
                    CIRCUIT_BREAK_DURATION.toMinutes() + "分钟后重试。");
            }
            
            // 保存异常状态到数据库
            saveExceptionState(chatId, e, ctx);
            
            // 缓存异常上下文
            cacheExceptionContext(chatId, e, ctx, errorCount);
            
        } catch (CircuitBreakerOpenException cbe) {
            // 重新抛出熔断异常
            throw cbe;
        } catch (Exception ex) {
            log.error("处理异常时发生错误", ex);
        }
    }

    /**
     * 记录异常日志
     */
    private void logException(String chatId, Exception e) {
        log.error("会话异常 - 会话ID: {}, 异常类型: {}, 消息: {}", 
            chatId, e.getClass().getSimpleName(), e.getMessage(), e);
    }

    /**
     * 增加错误计数
     */
    private int incrementErrorCount(String chatId) {
        // 清理过期的错误计数
        cleanupExpiredErrors(chatId);
        
        AtomicInteger counter = errorCounters.computeIfAbsent(chatId, k -> new AtomicInteger(0));
        int count = counter.incrementAndGet();
        
        // 更新最后错误时间
        lastErrorTime.put(chatId, Instant.now());
        
        log.info("会话{}错误计数增加到: {}", chatId, count);
        return count;
    }

    /**
     * 清理过期的错误计数
     */
    private void cleanupExpiredErrors(String chatId) {
        Instant lastError = lastErrorTime.get(chatId);
        if (lastError != null) {
            Duration timeSinceLastError = Duration.between(lastError, Instant.now());
            if (timeSinceLastError.compareTo(ERROR_WINDOW) > 0) {
                // 超过错误窗口期，重置计数
                errorCounters.remove(chatId);
                log.info("会话{}错误计数已重置（超过错误窗口期）", chatId);
            }
        }
    }

    /**
     * 检查是否应该熔断
     */
    public boolean shouldCircuitBreak(String chatId) {
        // 检查是否存在熔断器
        CircuitBreaker breaker = circuitBreakers.get(chatId);
        if (breaker != null && breaker.isOpen()) {
            // 检查熔断是否应该结束
            if (breaker.shouldReset()) {
                circuitBreakers.remove(chatId);
                errorCounters.remove(chatId);
                log.info("会话{}熔断器已重置", chatId);
                return false;
            }
            return true;
        }
        
        // 检查错误次数是否达到阈值
        AtomicInteger counter = errorCounters.get(chatId);
        if (counter != null && counter.get() >= ERROR_THRESHOLD) {
            // 触发熔断
            CircuitBreaker newBreaker = new CircuitBreaker(CIRCUIT_BREAK_DURATION);
            circuitBreakers.put(chatId, newBreaker);
            log.warn("会话{}触发熔断保护，错误次数: {}", chatId, counter.get());
            return true;
        }
        
        return false;
    }

    /**
     * 保存异常状态到数据库
     */
    private void saveExceptionState(String chatId, Exception e, StreamingContext ctx) {
        if (chatMessageMapper == null || ctx == null || ctx.getChatId() == null) {
            return;
        }
        
        try {
            SysAgentChatMessage errorMessage = new SysAgentChatMessage();
            errorMessage.setChatId(ctx.getChatId());
            errorMessage.setRole("system");
            errorMessage.setContent("系统异常: " + e.getMessage());
            errorMessage.setMessageType("error");
            errorMessage.setStatus("error");
            errorMessage.setMessageIndex(ctx.getAndIncrementMessageIndex());
            
            // 设置错误详情
            // 注：SysAgentChatMessage可能没有扩展字段，这里仅记录基本错误信息
            
            chatMessageMapper.insert(errorMessage);
            log.debug("已保存异常状态到数据库，会话ID: {}", chatId);
            
        } catch (Exception ex) {
            log.error("保存异常状态失败", ex);
        }
    }

    /**
     * 缓存异常上下文
     */
    private void cacheExceptionContext(String chatId, Exception e, StreamingContext ctx, int errorCount) {
        ExceptionContext context = new ExceptionContext();
        context.setLastException(e);
        context.setLastErrorTime(Instant.now());
        context.setErrorCount(errorCount);
        context.setStreamingContext(ctx);
        
        exceptionContexts.put(chatId, context);
    }

    /**
     * 异常恢复
     *
     * @param chatId 会话ID
     * @param ctx    流式上下文
     */
    public void recover(String chatId, StreamingContext ctx) {
        // 重置错误计数
        resetErrorCount(chatId);
        
        // 移除熔断器
        circuitBreakers.remove(chatId);
        
        // 恢复上下文
        restoreContext(chatId, ctx);
        
        log.info("会话{}已恢复正常状态", chatId);
    }

    /**
     * 重置错误计数
     */
    public void resetErrorCount(String chatId) {
        errorCounters.remove(chatId);
        lastErrorTime.remove(chatId);
        log.info("会话{}错误计数已重置", chatId);
    }

    /**
     * 恢复上下文
     */
    private void restoreContext(String chatId, StreamingContext ctx) {
        ExceptionContext cached = exceptionContexts.get(chatId);
        if (cached != null && cached.getStreamingContext() != null && ctx != null) {
            // 恢复必要的上下文信息
            StreamingContext cachedCtx = cached.getStreamingContext();
            ctx.setCurrentChatId(cachedCtx.getCurrentChatId());
            ctx.setUserId(cachedCtx.getUserId());
            ctx.setTenantId(cachedCtx.getTenantId());
            log.debug("已恢复会话{}的上下文信息", chatId);
        }
    }

    /**
     * 获取错误统计信息
     */
    public ErrorStatistics getErrorStatistics(String chatId) {
        ErrorStatistics stats = new ErrorStatistics();
        
        AtomicInteger counter = errorCounters.get(chatId);
        stats.setErrorCount(counter != null ? counter.get() : 0);
        
        CircuitBreaker breaker = circuitBreakers.get(chatId);
        stats.setCircuitBreakerOpen(breaker != null && breaker.isOpen());
        
        Instant lastError = lastErrorTime.get(chatId);
        stats.setLastErrorTime(lastError);
        
        ExceptionContext context = exceptionContexts.get(chatId);
        if (context != null && context.getLastException() != null) {
            stats.setLastErrorType(context.getLastException().getClass().getSimpleName());
            stats.setLastErrorMessage(context.getLastException().getMessage());
        }
        
        return stats;
    }

    /**
     * 清理过期的会话数据
     */
    public void cleanup() {
        Instant cutoffTime = Instant.now().minus(Duration.ofHours(1));
        
        // 清理过期的错误记录
        lastErrorTime.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoffTime));
        
        // 清理对应的计数器
        errorCounters.keySet().removeIf(chatId -> !lastErrorTime.containsKey(chatId));
        
        // 清理过期的熔断器
        circuitBreakers.entrySet().removeIf(entry -> entry.getValue().shouldReset());
        
        // 清理过期的异常上下文
        exceptionContexts.entrySet().removeIf(entry -> {
            ExceptionContext ctx = entry.getValue();
            return ctx.getLastErrorTime() != null && ctx.getLastErrorTime().isBefore(cutoffTime);
        });
        
        log.debug("清理完成，剩余会话数: {}", errorCounters.size());
    }

    /**
     * 熔断器类
     */
    private static class CircuitBreaker {
        private final Instant openTime;
        private final Duration duration;

        public CircuitBreaker(Duration duration) {
            this.openTime = Instant.now();
            this.duration = duration;
        }

        public boolean isOpen() {
            return !shouldReset();
        }

        public boolean shouldReset() {
            return Duration.between(openTime, Instant.now()).compareTo(duration) > 0;
        }
    }

    /**
     * 异常上下文类
     */
    private static class ExceptionContext {
        private Exception lastException;
        private Instant lastErrorTime;
        private int errorCount;
        private StreamingContext streamingContext;

        // Getters and setters
        public Exception getLastException() {
            return lastException;
        }

        public void setLastException(Exception lastException) {
            this.lastException = lastException;
        }

        public Instant getLastErrorTime() {
            return lastErrorTime;
        }

        public void setLastErrorTime(Instant lastErrorTime) {
            this.lastErrorTime = lastErrorTime;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public StreamingContext getStreamingContext() {
            return streamingContext;
        }

        public void setStreamingContext(StreamingContext streamingContext) {
            this.streamingContext = streamingContext;
        }
    }

    /**
     * 错误统计信息类
     */
    public static class ErrorStatistics {
        private int errorCount;
        private boolean circuitBreakerOpen;
        private Instant lastErrorTime;
        private String lastErrorType;
        private String lastErrorMessage;

        // Getters and setters
        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public boolean isCircuitBreakerOpen() {
            return circuitBreakerOpen;
        }

        public void setCircuitBreakerOpen(boolean circuitBreakerOpen) {
            this.circuitBreakerOpen = circuitBreakerOpen;
        }

        public Instant getLastErrorTime() {
            return lastErrorTime;
        }

        public void setLastErrorTime(Instant lastErrorTime) {
            this.lastErrorTime = lastErrorTime;
        }

        public String getLastErrorType() {
            return lastErrorType;
        }

        public void setLastErrorType(String lastErrorType) {
            this.lastErrorType = lastErrorType;
        }

        public String getLastErrorMessage() {
            return lastErrorMessage;
        }

        public void setLastErrorMessage(String lastErrorMessage) {
            this.lastErrorMessage = lastErrorMessage;
        }
    }

    /**
     * 熔断器异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}