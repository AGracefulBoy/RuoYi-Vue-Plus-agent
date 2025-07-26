package org.dromara.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务智能体相关DTO
 *
 * @author assistant
 */
public class TaskAgentDto {

    /**
     * 思维链步骤
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReasoningStep {
        /**
         * 步骤ID
         */
        private String stepId;

        /**
         * 步骤类型：thought(思考)、action(行动)、observation(观察)
         */
        private String stepType;

        /**
         * 步骤内容
         */
        private String content;

        /**
         * 执行时间
         */
        private LocalDateTime timestamp;

        /**
         * 是否完成
         */
        private Boolean completed;

        /**
         * 结果
         */
        private String result;
    }

    /**
     * 聊天消息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        /**
         * 消息类型：USER、AI、SYSTEM
         */
        private MessageType type;

        /**
         * 消息内容
         */
        private String content;

        /**
         * 时间戳
         */
        private LocalDateTime timestamp;

        /**
         * 思维链步骤（仅AI消息有）
         */
        private List<ReasoningStep> reasoningSteps;

        public ChatMessage(MessageType type, String content) {
            this.type = type;
            this.content = content;
            this.timestamp = LocalDateTime.now();
            this.reasoningSteps = new ArrayList<>();
        }

        public enum MessageType {
            USER, AI, SYSTEM
        }
    }

    /**
     * 任务记忆
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class TaskMemory {
        /**
         * 聊天历史
         */
        private List<ChatMessage> chatHistory;

        /**
         * 当前会话ID
         */
        private String sessionId;

        /**
         * 任务状态
         */
        private TaskStatus status;

        /**
         * 当前目标
         */
        private String currentGoal;

        /**
         * 执行计划
         */
        private List<String> executionPlan;

        /**
         * 已完成步骤
         */
        private List<String> completedSteps;

        public TaskMemory() {
            this.chatHistory = new ArrayList<>();
            this.status = TaskStatus.READY;
            this.executionPlan = new ArrayList<>();
            this.completedSteps = new ArrayList<>();
        }

        public void addMessage(ChatMessage message) {
            this.chatHistory.add(message);
        }

        public enum TaskStatus {
            READY("准备就绪"),
            PLANNING("制定计划"),
            EXECUTING("执行中"),
            WAITING_INPUT("等待输入"),
            COMPLETED("已完成"),
            FAILED("执行失败");

            private final String description;

            TaskStatus(String description) {
                this.description = description;
            }

            public String getDescription() {
                return description;
            }
        }
    }

    /**
     * ReAct响应结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReActResponse {
        /**
         * 响应内容
         */
        private String content;

        /**
         * 思维链步骤
         */
        private List<ReasoningStep> reasoningSteps;

        /**
         * 是否需要继续执行
         */
        private Boolean needsContinuation;

        /**
         * 下一步动作
         */
        private String nextAction;

        /**
         * 任务状态
         */
        private TaskMemory.TaskStatus taskStatus;
    }
}
