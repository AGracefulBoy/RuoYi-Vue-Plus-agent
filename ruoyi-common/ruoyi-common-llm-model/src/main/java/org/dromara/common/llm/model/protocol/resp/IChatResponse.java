package org.dromara.common.llm.model.protocol.resp;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
public class IChatResponse {

    // 主类的getter和setter方法
    private Result result;
    private Metadata metadata;

    // 内部类定义
    @Setter
    @Getter
    public static class Result {
        // getter和setter方法
        private ResultMetadata metadata;
        private Output output;

    }

    @Setter
    @Getter
    public static class ResultMetadata {
        // getter和setter方法
        private String finishReason;
        private List<Object> contentFilters;
        private boolean empty;

    }

    @Setter
    @Getter
    public static class Output {
        // getter和setter方法
        private String messageType;
        private OutputMetadata metadata;
        private List<Object> toolCalls;
        private List<Object> media;
        private Object prefix;
        private Object reasoningContent;
        private String text;

    }

    @Setter
    @Getter
    public static class OutputMetadata {
        // getter和setter方法
        private String finishReason;
        private String id;
        private String role;
        private String messageType;

    }

    @Setter
    @Getter
    public static class Metadata {
        // getter和setter方法
        private String id;
        private String model;
        private RateLimit rateLimit;
        private Usage usage;
        private List<Object> promptMetadata;
        private boolean empty;

    }

    @Setter
    @Getter
    public static class RateLimit {
        // getter和setter方法
        private int requestsRemaining;
        private String requestsReset;
        private int tokensRemaining;
        private String tokensReset;
        private int tokensLimit;
        private int requestsLimit;

    }

    @Setter
    @Getter
    public static class Usage {
        // getter和setter方法
        private Map<String, Object> nativeUsage;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;

    }

}
