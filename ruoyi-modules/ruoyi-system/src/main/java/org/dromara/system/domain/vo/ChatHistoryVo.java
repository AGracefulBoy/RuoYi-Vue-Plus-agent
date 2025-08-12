package org.dromara.system.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.system.domain.SysAgentChatMessage;

import java.util.List;

/**
 * 会话历史VO
 *
 * @author zhoudashuai
 */
@Data
public class ChatHistoryVo {

    /**
     * 会话ID
     */
    private Long chatId;

    /**
     * 会话ID（用于前端标识）
     */
    private String conversationId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 消息列表
     */
    private List<SysAgentChatMessage> messages;

    /**
     * Token使用情况
     */
    private TokenUsage tokenUsage;

    /**
     * Token使用情况
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenUsage {
        /**
         * 输入token数
         */
        private Integer inputTokens;

        /**
         * 输出token数
         */
        private Integer outputTokens;

        /**
         * 总token数
         */
        private Integer totalTokens;
    }
}