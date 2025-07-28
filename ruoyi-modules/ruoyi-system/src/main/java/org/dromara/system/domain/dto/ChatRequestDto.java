package org.dromara.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 聊天请求DTO
 *
 * @author zhoudashuai
 */
@Data
@Accessors(chain = true)
public class ChatRequestDto {

    /**
     * 智能体ID
     */
    @NotNull(message = "智能体ID不能为空")
    private Long agentId;

    /**
     * 用户问题
     */
    @NotBlank(message = "用户问题不能为空")
    private String message;

    /**
     * 聊天ID（可选，用于多轮对话）
     */
    private String chatId;

    /**
     * 是否流式响应
     */
    private Boolean stream = true;

    /**
     * 会话历史消息（用于上下文）
     */
    private List<ChatMessage> conversationHistory;

    /**
     * 模型参数配置
     */
    private ModelParams modelParams;

    /**
     * 是否包含思维链步骤
     */
    private Boolean includeThoughtSteps = true;

    /**
     * 追踪ID（用于调用链追踪）
     */
    private String traceId;

    /**
     * 用户自定义参数
     */
    private Map<String, Object> userParams;

    /**
     * 历史消息
     */
    @Data
    @Accessors(chain = true)
    public static class ChatMessage {
        /**
         * 角色（user、assistant、system、tool）
         */
        @NotBlank(message = "消息角色不能为空")
        private String role;

        /**
         * 消息内容
         */
        @NotBlank(message = "消息内容不能为空")
        private String content;

        /**
         * 消息类型（可选）
         */
        private String messageType;

        /**
         * 工具调用信息（可选）
         */
        private List<ToolCallInfo> toolCalls;
    }

    /**
     * 工具调用信息
     */
    @Data
    @Accessors(chain = true)
    public static class ToolCallInfo {
        /**
         * 工具名称
         */
        private String toolName;

        /**
         * 调用参数
         */
        private Map<String, Object> parameters;

        /**
         * 调用结果
         */
        private String result;
    }

    /**
     * 模型参数
     */
    @Data
    @Accessors(chain = true)
    public static class ModelParams {
        /**
         * 温度参数（0-2）
         */
        @Min(value = 0, message = "温度参数最小值为0")
        @Max(value = 2, message = "温度参数最大值为2")
        private Double temperature = 0.7;

        /**
         * 最大token数
         */
        @Min(value = 1, message = "最大token数必须大于0")
        @Max(value = 32000, message = "最大token数不能超过32000")
        private Integer maxTokens = 4096;

        /**
         * top_p参数（0-1）
         */
        @Min(value = 0, message = "top_p参数最小值为0")
        @Max(value = 1, message = "top_p参数最大值为1")
        private Double topP = 0.9;

        /**
         * 频率惩罚（-2到2）
         */
        @Min(value = -2, message = "频率惩罚最小值为-2")
        @Max(value = 2, message = "频率惩罚最大值为2")
        private Double frequencyPenalty = 0.0;

        /**
         * 存在惩罚（-2到2）
         */
        @Min(value = -2, message = "存在惩罚最小值为-2")
        @Max(value = 2, message = "存在惩罚最大值为2")
        private Double presencePenalty = 0.0;

        /**
         * 停止词列表
         */
        private List<String> stopSequences;

        /**
         * 模型配置ID（优先使用此配置）
         */
        private Long modelConfigId;
    }
}
