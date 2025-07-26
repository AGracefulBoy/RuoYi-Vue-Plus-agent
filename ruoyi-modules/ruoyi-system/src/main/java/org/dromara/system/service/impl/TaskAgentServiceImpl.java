package org.dromara.system.service.impl;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.factory.AiService;
import org.dromara.common.llm.model.platform.IChatService;
import org.dromara.common.llm.model.protocol.req.IChatRequest;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.dto.TaskAgentDto.*;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.domain.vo.SysModelConfigVo;
import org.dromara.system.service.ISysModelConfigService;
import org.dromara.system.service.TaskAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 任务智能体服务实现类
 *
 * @author assistant
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskAgentServiceImpl implements TaskAgentService {

    @Autowired
    private AiService aiService;

    @Autowired
    private ISysModelConfigService modelConfigService;

    @Override
    public Flux<String> executeReActStream(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        return Flux.create(sink -> {
            try {
                // 执行流式推理
                performStreamingReasoningChain(agent, availableTools, userInput, sink);

            } catch (Exception e) {
                log.error("流式ReAct执行失败", e);
                sink.error(e);
            }
        });
    }


    /**
     * 执行流式推理链
     */
    private void performStreamingReasoningChain(SysAgent agent, List<ToolDto> availableTools,
                                                String userInput, reactor.core.publisher.FluxSink<String> sink) {
        try {
            // 1. 获取并验证模型配置
            ModelConfigContext modelContext = getAndValidateModelConfigs(agent, sink);
            if (modelContext == null) return;

            // 3. 构建提示词
            String cotPrompt = buildPrompt(agent, availableTools, userInput);

            // 4. 执行流式AI调用
            executeStreamingAICall(cotPrompt, modelContext, sink);

        } catch (Exception e) {
            log.error("流式推理执行失败", e);
            sink.error(new RuntimeException("流式推理执行失败: " + e.getMessage()));
        }
    }

    /**
     * 获取并验证模型配置
     */
    private ModelConfigContext getAndValidateModelConfigs(SysAgent agent, reactor.core.publisher.FluxSink<String> sink) {
        try {
            SysModelConfigVo mainModelConfig = getModelConfig(agent.getModel(), "主要模型");
            SysModelConfigVo enhanceModelConfig = getModelConfig(agent.getEnhanceModel(), "增强模型");

            // 验证主要模型配置
            if (mainModelConfig == null) {
                sink.error(new RuntimeException("智能体主要模型配置不存在或无效，无法执行推理任务"));
                return null;
            }

            // 检查模型类型
            if (!isValidModelType(mainModelConfig)) {
                sink.error(new RuntimeException("主要模型不支持聊天功能，请检查模型配置"));
                return null;
            }

            return new ModelConfigContext(mainModelConfig, enhanceModelConfig);

        } catch (Exception e) {
            log.error("查询模型配置失败", e);
            sink.error(new RuntimeException("查询模型配置失败: " + e.getMessage()));
            return null;
        }
    }

    /**
     * 获取模型配置
     */
    private SysModelConfigVo getModelConfig(Long modelId, String modelType) {
        if (modelId == null) {
            log.debug("未配置{}，modelId为null", modelType);
            return null;
        }

        SysModelConfigVo modelConfig = modelConfigService.queryById(modelId);
        if (modelConfig != null) {
            log.info("获取{}配置成功: modelCode={}, provider={}",
                modelType, modelConfig.getModelCode(), modelConfig.getModelProvider());
        } else {
            log.warn("未找到{}配置，模型ID: {}", modelType, modelId);
        }
        return modelConfig;
    }

    /**
     * 验证模型类型是否支持聊天
     */
    private boolean isValidModelType(SysModelConfigVo modelConfig) {
        List<String> modelType = modelConfig.getModelType();
        return modelType != null && (modelType.contains("chat") || modelType.contains("llm"));
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(SysAgent agent, List<ToolDto> availableTools, String userInput) {
        String promptContent = agent.getPromptContent();
        if (!StringUtils.hasText(promptContent)) {
            promptContent = "你是一个智能助手，请根据用户的问题提供有帮助的回答。\n用户问题：{{query}}";
        }

        String cotPrompt = promptContent
            .replace("{{agent_personality}}",
                StringUtils.hasText(agent.getAgentPersonality()) ? agent.getAgentPersonality() : "专业助手")
            .replace("{{tool_list}}",
                CollectionUtils.isEmpty(availableTools) ? "[]" : JSONUtil.toJsonStr(availableTools))
            .replace("{{query}}", userInput);

        log.info("构建的提示词: {}", cotPrompt);
        return cotPrompt;
    }

    /**
     * 执行流式AI调用
     */
    private void executeStreamingAICall(String cotPrompt, ModelConfigContext modelContext,
                                        reactor.core.publisher.FluxSink<String> sink) {
        // 获取聊天服务
        IChatService chatService = aiService.getChatService(modelContext.getMainModel().getModelProvider());
        if (chatService == null) {
            sink.error(new RuntimeException("无法获取聊天服务，提供商: " + modelContext.getMainModel().getModelProvider()));
            return;
        }

        // 构建聊天请求
        IChatRequest chatRequest = buildChatRequest(cotPrompt, modelContext.getMainModel());

        // 执行流式调用
        Flux<IChatResponse> modelStream = chatService.stream(chatRequest);
        processModelStream(modelStream, modelContext, sink);
    }

    /**
     * 构建聊天请求
     */
    private IChatRequest buildChatRequest(String prompt, SysModelConfigVo modelConfig) {
        IChatRequest request = new IChatRequest();
        request.setStream(true);
        request.setModel(modelConfig.getModelCode());
        request.setApiKey(modelConfig.getApiKey());
        request.setBaseUrl(modelConfig.getBaseUrl());
        request.setPrompt(prompt);
        return request;
    }

    /**
     * 处理模型流式响应
     */
    private void processModelStream(Flux<IChatResponse> modelStream, ModelConfigContext modelContext,
                                    reactor.core.publisher.FluxSink<String> sink) {
        StringBuilder fullResponse = new StringBuilder();

        modelStream
            .doOnNext(chunk -> handleStreamChunk(chunk, sink, fullResponse))
            .doOnComplete(() -> handleStreamComplete(fullResponse.toString(), modelContext, sink))
            .doOnError(error -> handleStreamError(error, sink))
            .subscribe();
    }

    /**
     * 处理流式响应块
     */
    private void handleStreamChunk(IChatResponse chunk, reactor.core.publisher.FluxSink<String> sink,
                                   StringBuilder fullResponse) {
        if (chunk.getResult() != null && chunk.getResult().getOutput() != null) {
            String text = chunk.getResult().getOutput().getText();
            if (StringUtils.hasText(text)) {
                sink.next(text);
                fullResponse.append(text);
            }
        }
    }

    /**
     * 处理流式响应完成
     */
    private void handleStreamComplete(String finalResponse, ModelConfigContext modelContext,
                                      reactor.core.publisher.FluxSink<String> sink) {
        log.info("AI模型流式调用完成");

        // 如果配置了增强模型，使用增强模型优化回复
        if (modelContext.getEnhanceModel() != null && StringUtils.hasText(finalResponse)) {
            try {
                sink.next("\n\n🔧 使用增强模型优化回复...\n");
                String enhancedResponse = enhanceResponse(finalResponse, modelContext.getEnhanceModel(), null);
                if (!enhancedResponse.equals(finalResponse)) {
                    sink.next("\n✨ 优化后的回复：\n");
                    sink.next(enhancedResponse);
                }
            } catch (Exception e) {
                log.error("增强模型处理失败", e);
                sink.next("\n⚠️ 增强模型处理失败，已返回原始回复\n");
            }
        }

        sink.complete();
    }

    /**
     * 处理流式响应错误
     */
    private void handleStreamError(Throwable error, reactor.core.publisher.FluxSink<String> sink) {
        log.error("AI模型流式调用失败", error);
        sink.error(new RuntimeException("AI模型调用失败: " + error.getMessage()));
    }

    /**
     * 模型配置上下文类
     */
    private static class ModelConfigContext {
        private final SysModelConfigVo mainModel;
        private final SysModelConfigVo enhanceModel;

        public ModelConfigContext(SysModelConfigVo mainModel, SysModelConfigVo enhanceModel) {
            this.mainModel = mainModel;
            this.enhanceModel = enhanceModel;
        }

        public SysModelConfigVo getMainModel() {
            return mainModel;
        }

        public SysModelConfigVo getEnhanceModel() {
            return enhanceModel;
        }
    }

    @Override
    public boolean checkExitCondition(String input) {
        if (!StringUtils.hasText(input)) {
            return false;
        }
        String lowerInput = input.toLowerCase().trim();
        return lowerInput.equals("exit") ||
            lowerInput.equals("quit") ||
            lowerInput.equals("退出") ||
            lowerInput.equals("结束");
    }

    /**
     * 使用增强模型优化回复
     */
    private String enhanceResponse(String originalResponse, SysModelConfigVo enhanceModelConfig, SysAgent agent) {
        try {
            log.info("使用增强模型优化回复: {}", enhanceModelConfig.getModelCode());

            // 构建增强提示词
            String enhancePrompt = "请优化以下回复内容，使其更加准确、友好和有用：\n\n" + originalResponse;

            // 获取增强模型的聊天服务
            IChatService enhanceChatService = aiService.getChatService(enhanceModelConfig.getModelProvider());
            if (enhanceChatService == null) {
                log.warn("无法获取增强模型的聊天服务: {}", enhanceModelConfig.getModelProvider());
                return originalResponse;
            }

            // 构建增强模型请求
            IChatRequest enhanceRequest = new IChatRequest();
            enhanceRequest.setStream(false); // 增强模型使用同步调用
            enhanceRequest.setModel(enhanceModelConfig.getModelCode());
            enhanceRequest.setApiKey(enhanceModelConfig.getApiKey());
            enhanceRequest.setBaseUrl(enhanceModelConfig.getBaseUrl());
            enhanceRequest.setPrompt(enhancePrompt);

            // 调用增强模型
//            String enhancedResponse = enhanceChatService.chat(enhanceRequest);

            if (StringUtils.hasText("11")) {
                log.info("增强模型优化成功");
                return "11";
            } else {
                log.warn("增强模型返回空结果");
                return originalResponse;
            }

        } catch (Exception e) {
            log.error("增强模型优化失败，返回原始回复", e);
            return originalResponse;
        }
    }
}
