package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;
import java.util.List;

/**
 * 智能体会话记录表 sys_agent_chat
 *
 * @author zhoudashuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_agent_chat", autoResultMap = true)
public class SysAgentChat extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "chat_id")
    private Long chatId;

    /**
     * 会话UUID（用于前端标识）
     */
    private String chatUuid;

    /**
     * 智能体ID
     */
    private Long agentId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 会话标题
     */
    private String chatTitle;

    /**
     * 会话状态（active活跃、archived归档、deleted删除）
     */
    private String status;

    /**
     * 消息总数
     */
    private Integer messageCount;

    /**
     * 总token消耗
     */
    private Integer totalTokens;

    /**
     * 输入token消耗
     */
    private Integer inputTokens;

    /**
     * 输出token消耗
     */
    private Integer outputTokens;

    /**
     * 会话元数据（JSON格式，存储额外信息）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private ChatMetadata metadata;

    /**
     * 最后活跃时间
     */
    private Date lastActiveTime;

    /**
     * 会话开始时间
     */
    private Date startTime;

    /**
     * 会话结束时间
     */
    private Date endTime;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 会话元数据内部类
     */
    @Data
    public static class ChatMetadata {
        /**
         * 模型配置ID
         */
        private Long modelConfigId;

        /**
         * 温度参数
         */
        private Double temperature;

        /**
         * 最大token数
         */
        private Integer maxTokens;

        /**
         * top_p参数
         */
        private Double topP;

        /**
         * 上下文窗口大小
         */
        private Integer contextWindow;

        /**
         * 使用的工具ID列表
         */
        private List<Long> toolIds;

        /**
         * 使用的知识库ID列表
         */
        private List<Long> knowledgeBaseIds;

        /**
         * 使用的数据源ID列表
         */
        private List<Long> datasourceIds;

        /**
         * 其他扩展参数
         */
        private String extraParams;
    }
}