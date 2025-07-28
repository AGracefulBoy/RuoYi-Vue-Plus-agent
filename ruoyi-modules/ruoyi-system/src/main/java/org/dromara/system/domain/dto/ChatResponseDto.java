package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 聊天响应DTO
 *
 * @author zhoudashuai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ChatResponseDto {

    /**
     * 会话ID
     */
    private String chatId;

    /**
     * 消息ID
     */
    private Long messageId;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 响应类型（text、thought_process、final_answer、error）
     */
    private String responseType;

    /**
     * 思维链步骤（如果includeThoughtSteps为true）
     */
    private List<ThoughtStep> thoughtSteps;

    /**
     * 工具调用结果
     */
    private List<ToolCallResult> toolCallResults;

    /**
     * token使用情况
     */
    private TokenUsage tokenUsage;

    /**
     * 性能指标
     */
    private PerformanceMetrics performanceMetrics;

    /**
     * 响应元数据
     */
    private ResponseMetadata metadata;

    /**
     * 响应状态（success、partial、error）
     */
    private String status;

    /**
     * 错误信息（如果有）
     */
    private String errorMessage;

    /**
     * 响应时间戳
     */
    private Date timestamp;

    /**
     * 思维链步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ThoughtStep {
        /**
         * 步骤类型（thought、action、observation、reflection、final_answer）
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
         * 相关工具调用ID（如果有）
         */
        private String toolCallId;

        /**
         * 步骤耗时（毫秒）
         */
        private Long duration;

        /**
         * 步骤状态（pending、completed、failed）
         */
        private String status;

        /**
         * 步骤时间戳
         */
        private Date timestamp;
    }

    /**
     * 工具调用结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallResult {
        /**
         * 工具调用ID
         */
        private String toolCallId;

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
        private Object result;

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

        /**
         * 调用时间戳
         */
        private Date timestamp;
    }

    /**
     * Token使用情况
     */
    @Data
    @Builder
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

        /**
         * 剩余token数（如果有限制）
         */
        private Integer remainingTokens;

        /**
         * token成本估算
         */
        private Double estimatedCost;
    }

    /**
     * 性能指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetrics {
        /**
         * 总响应时间（毫秒）
         */
        private Long totalResponseTime;

        /**
         * 模型推理时间（毫秒）
         */
        private Long modelInferenceTime;

        /**
         * 工具调用总时间（毫秒）
         */
        private Long toolCallTime;

        /**
         * 数据处理时间（毫秒）
         */
        private Long dataProcessingTime;

        /**
         * 首次响应时间（毫秒，流式响应）
         */
        private Long firstResponseTime;

        /**
         * ReAct循环次数
         */
        private Integer reactIterations;
    }

    /**
     * 响应元数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseMetadata {
        /**
         * 使用的模型名称
         */
        private String modelName;

        /**
         * 模型版本
         */
        private String modelVersion;

        /**
         * 追踪ID
         */
        private String traceId;

        /**
         * 智能体ID
         */
        private Long agentId;

        /**
         * 智能体名称
         */
        private String agentName;

        /**
         * 用户ID
         */
        private Long userId;

        /**
         * 是否使用缓存
         */
        private Boolean cached;

        /**
         * 使用的工具ID列表
         */
        private List<Long> usedToolIds;

        /**
         * 使用的知识库ID列表
         */
        private List<Long> usedKnowledgeBaseIds;

        /**
         * 扩展信息
         */
        private Map<String, Object> extras;
    }
}