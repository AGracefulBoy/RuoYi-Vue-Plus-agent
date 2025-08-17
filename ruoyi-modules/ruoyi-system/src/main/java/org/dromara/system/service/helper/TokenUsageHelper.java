package org.dromara.system.service.helper;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.TokenUsageDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.FluxSink;

/**
 * Token使用辅助类
 * 负责处理Token的统计、累计和报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageHelper {

    /**
     * 累计Token使用量
     *
     * @param response 聊天响应
     * @param ctx      流式上下文
     */
    public void accumulateTokenUsage(IChatResponse response, StreamingContext ctx) {
        if (response != null && response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            IChatResponse.Usage currentUsage = response.getMetadata().getUsage();
            IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();

            // 累加token使用量
            totalUsage.setPromptTokens(totalUsage.getPromptTokens() + currentUsage.getPromptTokens());
            totalUsage.setCompletionTokens(totalUsage.getCompletionTokens() + currentUsage.getCompletionTokens());
            totalUsage.setTotalTokens(totalUsage.getTotalTokens() + currentUsage.getTotalTokens());

            log.info("Token usage for this step - Prompt: {}, Completion: {}, Total: {}",
                currentUsage.getPromptTokens(),
                currentUsage.getCompletionTokens(),
                currentUsage.getTotalTokens());

            log.info("Cumulative token usage - Prompt: {}, Completion: {}, Total: {}",
                totalUsage.getPromptTokens(),
                totalUsage.getCompletionTokens(),
                totalUsage.getTotalTokens());
        }
    }

    /**
     * 获取Token使用量对象
     *
     * @param ctx 流式上下文
     * @return Token使用量DTO
     */
    public TokenUsageDto getTokenUsageObject(StreamingContext ctx) {
        IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();
        return TokenUsageDto.fromUsage(totalUsage);
    }

    /**
     * 报告总Token使用量
     *
     * @param sink 流式输出
     * @param ctx  流式上下文
     */
    public void reportTotalTokenUsage(FluxSink<StreamMessageResponseDto> sink, StreamingContext ctx) {
        // 获取token使用量对象
        TokenUsageDto tokenUsage = getTokenUsageObject(ctx);

        // 只有在有使用量时才报告
        if (tokenUsage.getTotalTokens() > 0) {
            // 将对象转换为JSON字符串
            String tokenUsageJson = JSONUtil.toJsonStr(tokenUsage);

            log.info("Total token usage for entire ReAct loop: {}", tokenUsageJson);
        }
    }

    /**
     * 创建Token使用信息的元数据
     *
     * @param tokenUsage Token使用情况
     * @return 元数据映射
     */
    public java.util.Map<String, Object> createTokenMetadata(IChatResponse.Usage tokenUsage) {
        java.util.Map<String, Object> tokenInfo = new java.util.HashMap<>();
        if (tokenUsage != null) {
            tokenInfo.put("promptTokens", tokenUsage.getPromptTokens());
            tokenInfo.put("completionTokens", tokenUsage.getCompletionTokens());
            tokenInfo.put("totalTokens", tokenUsage.getTotalTokens());
        }
        return tokenInfo;
    }

    /**
     * 计算Token成本（如果需要的话）
     *
     * @param tokenUsage Token使用情况
     * @param pricePerK  每千个Token的价格
     * @return 成本
     */
    public double calculateCost(IChatResponse.Usage tokenUsage, double pricePerK) {
        if (tokenUsage == null) {
            return 0.0;
        }
        return (tokenUsage.getTotalTokens() / 1000.0) * pricePerK;
    }

    /**
     * 检查是否超过Token限制
     *
     * @param ctx       流式上下文
     * @param maxTokens 最大Token数
     * @return 是否超过限制
     */
    public boolean isTokenLimitExceeded(StreamingContext ctx, int maxTokens) {
        IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();
        return totalUsage.getTotalTokens() >= maxTokens;
    }

    /**
     * 重置Token使用量
     *
     * @param ctx 流式上下文
     */
    public void resetTokenUsage(StreamingContext ctx) {
        IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();
        totalUsage.setPromptTokens(0);
        totalUsage.setCompletionTokens(0);
        totalUsage.setTotalTokens(0);
    }

    /**
     * 记录Token使用情况到日志
     *
     * @param ctx      流式上下文
     * @param stepName 步骤名称
     */
    public void logTokenUsage(StreamingContext ctx, String stepName) {
        IChatResponse.Usage totalUsage = ctx.getTotalTokenUsage();
        log.info("Token usage at {} - Prompt: {}, Completion: {}, Total: {}",
            stepName,
            totalUsage.getPromptTokens(),
            totalUsage.getCompletionTokens(),
            totalUsage.getTotalTokens());
    }
}