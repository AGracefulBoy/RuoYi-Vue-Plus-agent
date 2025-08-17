package org.dromara.system.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.StreamMessageResponseDto;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 错误消息构建器
 * 统一处理各种异常情况，生成用户友好的错误消息
 *
 * @author system
 */
@Slf4j
public class ErrorMessageBuilder {

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        TIMEOUT("timeout", "处理超时"),
        MODEL_ERROR("model_error", "模型服务异常"),
        TOOL_ERROR("tool_error", "工具执行异常"),
        NETWORK_ERROR("network_error", "网络连接异常"),
        VALIDATION_ERROR("validation_error", "参数验证失败"),
        PERMISSION_ERROR("permission_error", "权限不足"),
        RESOURCE_ERROR("resource_error", "资源不可用"),
        SYSTEM_ERROR("system_error", "系统内部错误"),
        UNKNOWN("unknown", "未知错误");

        private final String code;
        private final String description;

        ErrorType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 构建错误消息
     *
     * @param e       异常对象
     * @param chatId  会话ID
     * @param ctx     流式上下文
     * @return 错误消息DTO
     */
    public static StreamMessageResponseDto buildErrorMessage(
            Exception e, String chatId, StreamingContext ctx) {
        
        ErrorType errorType = classifyError(e);
        String userFriendlyMessage = getUserFriendlyMessage(errorType, e);
        
        // 构建额外信息
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("error", true);
        extraInfo.put("errorType", errorType.getCode());
        extraInfo.put("errorCode", getErrorCode(e));
        extraInfo.put("errorDetail", getErrorDetail(e));
        extraInfo.put("suggestion", getErrorSuggestion(errorType));
        extraInfo.put("canRetry", isRetryable(e));
        extraInfo.put("timestamp", System.currentTimeMillis());
        
        // 记录错误日志
        log.error("构建错误消息 - 类型: {}, 会话: {}, 消息: {}", 
            errorType, chatId, e.getMessage(), e);
        
        return StreamMessageResponseDto.builder()
                .message(StreamMessageResponseDto.MessageData.builder()
                        .role("assistant")
                        .type("error")
                        .content(userFriendlyMessage)
                        .contentType("text")
                        .messageId(generateErrorMessageId())
                        .extraInfo(extraInfo)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(true)
                .index(ctx != null ? ctx.getAndIncrementMessageIndex() : 0)
                .chatId(chatId)
                .errorType(errorType.getCode())
                .canRetry(isRetryable(e))
                .build();
    }

    /**
     * 分类错误类型
     */
    private static ErrorType classifyError(Exception e) {
        if (e instanceof TimeoutException || e instanceof SocketTimeoutException) {
            return ErrorType.TIMEOUT;
        }
        
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("模型") || lowerMessage.contains("model") || 
                lowerMessage.contains("ai") || lowerMessage.contains("llm")) {
                return ErrorType.MODEL_ERROR;
            }
            if (lowerMessage.contains("工具") || lowerMessage.contains("tool") || 
                lowerMessage.contains("执行")) {
                return ErrorType.TOOL_ERROR;
            }
            if (lowerMessage.contains("网络") || lowerMessage.contains("network") || 
                lowerMessage.contains("connection")) {
                return ErrorType.NETWORK_ERROR;
            }
            if (lowerMessage.contains("权限") || lowerMessage.contains("permission") || 
                lowerMessage.contains("access")) {
                return ErrorType.PERMISSION_ERROR;
            }
            if (lowerMessage.contains("参数") || lowerMessage.contains("validation") || 
                lowerMessage.contains("invalid")) {
                return ErrorType.VALIDATION_ERROR;
            }
            if (lowerMessage.contains("资源") || lowerMessage.contains("resource") || 
                lowerMessage.contains("not found")) {
                return ErrorType.RESOURCE_ERROR;
            }
        }
        
        // 根据异常类名判断
        String className = e.getClass().getSimpleName();
        if (className.contains("Timeout")) {
            return ErrorType.TIMEOUT;
        }
        if (className.contains("Network") || className.contains("Socket")) {
            return ErrorType.NETWORK_ERROR;
        }
        
        return ErrorType.UNKNOWN;
    }

    /**
     * 获取用户友好的错误消息
     */
    private static String getUserFriendlyMessage(ErrorType errorType, Exception e) {
        switch (errorType) {
            case TIMEOUT:
                return "处理请求超时，请稍后重试。如果问题持续存在，请尝试简化您的问题。";
            case MODEL_ERROR:
                return "AI模型服务暂时不可用，请稍后重试。我们正在努力解决这个问题。";
            case TOOL_ERROR:
                return "工具执行遇到问题，请检查输入参数后重试。";
            case NETWORK_ERROR:
                return "网络连接出现问题，请检查您的网络连接后重试。";
            case VALIDATION_ERROR:
                return "输入参数有误，请检查后重新提交。";
            case PERMISSION_ERROR:
                return "您没有执行此操作的权限，请联系管理员。";
            case RESOURCE_ERROR:
                return "请求的资源暂时不可用，请稍后重试。";
            case SYSTEM_ERROR:
                return "系统遇到内部错误，我们已记录此问题，请稍后重试。";
            default:
                return "处理请求时遇到问题，请稍后重试。如果问题持续存在，请联系技术支持。";
        }
    }

    /**
     * 获取错误代码
     */
    private static String getErrorCode(Exception e) {
        // 生成唯一的错误代码，便于追踪
        return "ERR_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(e.getClass().getName().hashCode()).toUpperCase();
    }

    /**
     * 获取错误详情（用于调试）
     */
    private static String getErrorDetail(Exception e) {
        StringBuilder detail = new StringBuilder();
        detail.append("异常类型: ").append(e.getClass().getName());
        
        if (e.getMessage() != null) {
            detail.append("\n原始消息: ").append(e.getMessage());
        }
        
        if (e.getCause() != null) {
            detail.append("\n根本原因: ").append(e.getCause().getMessage());
        }
        
        // 获取堆栈跟踪的前3行
        StackTraceElement[] stackTrace = e.getStackTrace();
        if (stackTrace.length > 0) {
            detail.append("\n发生位置: ");
            for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                if (i > 0) detail.append("\n    ");
                detail.append(stackTrace[i].toString());
            }
        }
        
        return detail.toString();
    }

    /**
     * 获取错误处理建议
     */
    private static String getErrorSuggestion(ErrorType errorType) {
        switch (errorType) {
            case TIMEOUT:
                return "请尝试：1) 简化您的问题 2) 分步骤处理复杂任务 3) 稍后重试";
            case MODEL_ERROR:
                return "请尝试：1) 稍后重试 2) 使用其他模型 3) 联系技术支持";
            case TOOL_ERROR:
                return "请尝试：1) 检查工具参数 2) 使用其他工具 3) 手动执行操作";
            case NETWORK_ERROR:
                return "请尝试：1) 检查网络连接 2) 刷新页面 3) 稍后重试";
            case VALIDATION_ERROR:
                return "请尝试：1) 检查输入格式 2) 查看帮助文档 3) 使用示例输入";
            case PERMISSION_ERROR:
                return "请尝试：1) 重新登录 2) 联系管理员 3) 检查账户权限";
            case RESOURCE_ERROR:
                return "请尝试：1) 稍后重试 2) 使用其他资源 3) 联系技术支持";
            default:
                return "请尝试：1) 刷新页面 2) 稍后重试 3) 联系技术支持";
        }
    }

    /**
     * 判断是否可重试
     */
    private static boolean isRetryable(Exception e) {
        ErrorType errorType = classifyError(e);
        
        // 以下错误类型通常可以重试
        switch (errorType) {
            case TIMEOUT:
            case NETWORK_ERROR:
            case RESOURCE_ERROR:
            case MODEL_ERROR:
                return true;
            case VALIDATION_ERROR:
            case PERMISSION_ERROR:
                return false;
            default:
                // 默认允许重试，但限制次数
                return true;
        }
    }

    /**
     * 生成错误消息ID
     */
    private static String generateErrorMessageId() {
        return "error_msg_" + System.currentTimeMillis() + "_" + 
               (int) (Math.random() * 10000);
    }

    /**
     * 构建重试消息
     */
    public static StreamMessageResponseDto buildRetryMessage(
            String operation, int attemptNumber, String chatId, StreamingContext ctx) {
        
        String message = String.format("⏳ 正在重试%s（第%d次尝试）...", 
            operation, attemptNumber);
        
        Map<String, Object> extraInfo = new HashMap<>();
        extraInfo.put("retry", true);
        extraInfo.put("attemptNumber", attemptNumber);
        extraInfo.put("operation", operation);
        
        return StreamMessageResponseDto.builder()
                .message(StreamMessageResponseDto.MessageData.builder()
                        .role("system")
                        .type("info")
                        .content(message)
                        .contentType("text")
                        .messageId(generateErrorMessageId())
                        .extraInfo(extraInfo)
                        .contentTime(System.currentTimeMillis())
                        .build())
                .isFinish(false)
                .index(ctx != null ? ctx.getAndIncrementMessageIndex() : 0)
                .chatId(chatId)
                .build();
    }
}