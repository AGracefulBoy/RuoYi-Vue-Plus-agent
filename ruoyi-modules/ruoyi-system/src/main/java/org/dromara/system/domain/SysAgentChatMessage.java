package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 智能体会话消息表 sys_agent_chat_message
 *
 * @author zhoudashuai
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_agent_chat_message", autoResultMap = true)
public class SysAgentChatMessage extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "message_id")
    private Long messageId;

    /**
     * 会话ID
     */
    private Long chatId;

    /**
     * 角色（user用户、assistant助手、system系统、tool工具）
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（text文本、thought思考、action行动、observation观察、final_answer最终答案）
     */
    private String messageType;

    /**
     * 父消息ID（用于构建消息链）
     */
    private Long parentMessageId;

    /**
     * 消息序号（在会话中的顺序）
     */
    private Integer messageIndex;

    /**
     * token消耗
     */
    private Integer tokenCount;

    /**
     * 思维链步骤（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ThoughtStep> thoughtSteps;

    /**
     * 工具调用信息（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<ToolCall> toolCalls;

    /**
     * 消息元数据（JSON格式）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private MessageMetadata metadata;

    /**
     * 消息状态（pending处理中、completed完成、failed失败）
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 处理耗时（毫秒）
     */
    private Long processingTime;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 思维链步骤
     */
    @Data
    public static class ThoughtStep {
        /**
         * 步骤类型（thought、action、observation、reflection）
         */
        private String stepType;

        /**
         * 步骤内容
         */
        private String content;

        /**
         * 步骤序号
         */
        private Integer stepIndex;

        /**
         * 耗时（毫秒）
         */
        private Long duration;

        /**
         * 是否成功
         */
        private Boolean success;

        /**
         * 错误信息
         */
        private String error;
    }

    /**
     * 工具调用信息
     */
    @Data
    public static class ToolCall {
        /**
         * 工具ID
         */
        private Long toolId;

        /**
         * 工具名称
         */
        private String toolName;

        /**
         * 工具类型（tool、knowledge_base、datasource）
         */
        private String toolType;

        /**
         * 调用参数
         */
        private Map<String, Object> parameters;

        /**
         * 调用结果
         */
        private String result;

        /**
         * 是否成功
         */
        private Boolean success;

        /**
         * 耗时（毫秒）
         */
        private Long duration;

        /**
         * 错误信息
         */
        private String error;
    }

    /**
     * 消息元数据
     */
    @Data
    public static class MessageMetadata {
        /**
         * 模型名称
         */
        private String modelName;

        /**
         * 追踪ID
         */
        private String traceId;

        /**
         * 是否流式响应
         */
        private Boolean stream;

        /**
         * 引用的消息ID列表
         */
        private List<Long> references;

        /**
         * 其他扩展信息
         */
        private Map<String, Object> extras;
        
        /**
         * Token使用信息
         */
        private Map<String, Object> tokenUsage;
    }
}