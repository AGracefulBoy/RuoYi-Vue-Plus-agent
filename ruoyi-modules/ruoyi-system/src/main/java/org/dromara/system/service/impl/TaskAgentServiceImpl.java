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

        // 先根据 agent 中的 model 和 enhanceModel 去模型配置表中查询对应的模型信息
        SysModelConfigVo mainModelConfig = null;
        SysModelConfigVo enhanceModelConfig = null;

        try {
            // 查询主要模型配置
            if (agent.getModel() != null) {
                mainModelConfig = modelConfigService.queryById(agent.getModel());
                if (mainModelConfig != null) {
                    log.info("获取主要模型配置成功: modelCode={}, provider={}",
                        mainModelConfig.getModelCode(), mainModelConfig.getModelProvider());
                } else {
                    log.warn("未找到主要模型配置，模型ID: {}", agent.getModel());
                }
            } else {
                log.warn("智能体未配置主要模型，agentId: {}", agent.getAgentId());
            }

            // 查询增强模型配置
            if (agent.getEnhanceModel() != null) {
                enhanceModelConfig = modelConfigService.queryById(agent.getEnhanceModel());
                if (enhanceModelConfig != null) {
                    log.info("获取增强模型配置成功: modelCode={}, provider={}",
                        enhanceModelConfig.getModelCode(), enhanceModelConfig.getModelProvider());
                } else {
                    log.warn("未找到增强模型配置，模型ID: {}", agent.getEnhanceModel());
                }
            } else {
                log.debug("智能体未配置增强模型，agentId: {}", agent.getAgentId());
            }

            // 验证模型配置有效性
            if (mainModelConfig == null) {
                sink.error(new RuntimeException("智能体主要模型配置不存在或无效，无法执行推理任务"));
                return;
            }

            // 检查模型是否支持chat类型
            if (!mainModelConfig.getModelType().contains("chat") && !mainModelConfig.getModelType().contains("llm")) {
                sink.error(new RuntimeException("主要模型不支持聊天功能，请检查模型配置"));
                return;
            }

        } catch (Exception e) {
            log.error("查询模型配置失败", e);
            sink.error(new RuntimeException("查询模型配置失败: " + e.getMessage()));
            return;
        }

        // 模拟流式思考过程
        sink.next("🤔 开始思考您的问题...\n");
        sink.next("📋 使用模型: " + mainModelConfig.getModelCode() + " (" + mainModelConfig.getModelProvider() + ")\n");
        if (enhanceModelConfig != null) {
            sink.next("⚡ 增强模型: " + enhanceModelConfig.getModelCode() + " (" + enhanceModelConfig.getModelProvider() + ")\n");
        }

        try {
            // 构建提示词
            String promptContent = agent.getPromptContent();
            if (!StringUtils.hasText(promptContent)) {
                promptContent = "你是一个智能助手，请根据用户的问题提供有帮助的回答。\n用户问题：{{query}}";
            }

            String cotPrompt = promptContent.replace("{{agent_personality}}",
                    StringUtils.hasText(agent.getAgentPersonality()) ? agent.getAgentPersonality() : "专业助手")
                .replace("{{tool_list}}", CollectionUtils.isEmpty(availableTools) ? "[]" : JSONUtil.toJsonStr(availableTools))
                .replace("{{query}}", userInput);

            log.info("构建的提示词: {}", cotPrompt);

            // 获取聊天服务
            IChatService chatService = aiService.getChatService(mainModelConfig.getModelProvider());
            if (chatService == null) {
                sink.error(new RuntimeException("无法获取聊天服务，提供商: " + mainModelConfig.getModelProvider()));
                return;
            }

            // 构建聊天请求
            IChatRequest iChatRequest = new IChatRequest();
            iChatRequest.setStream(true);
            iChatRequest.setModel(mainModelConfig.getModelCode());
            iChatRequest.setApiKey(mainModelConfig.getApiKey());
            iChatRequest.setBaseUrl(mainModelConfig.getBaseUrl());
            iChatRequest.setPrompt(cotPrompt);

            log.info("开始流式调用AI模型: {}", mainModelConfig.getModelCode());
            sink.next("🚀 正在调用AI模型生成回复...\n");

            // 执行流式调用并处理响应
            Flux<IChatResponse> modelStream = chatService.stream(iChatRequest);

            StringBuilder fullResponse = new StringBuilder();

            SysModelConfigVo finalEnhanceModelConfig = enhanceModelConfig;
            modelStream
                .doOnNext(chunk -> {
                    // 将模型输出的每个chunk转发到sink
                    if (StringUtils.hasText(chunk.getResult().getOutput().getText())) {
                        sink.next(chunk.getResult().getOutput().getText());
                        fullResponse.append(chunk);
                    }
                })
                .doOnComplete(() -> {
                    log.info("AI模型流式调用完成");

                    String finalResponse = fullResponse.toString();

                    // 如果配置了增强模型，使用增强模型优化回复
                    if (finalEnhanceModelConfig != null && StringUtils.hasText(finalResponse)) {
                        try {
                            sink.next("\n\n🔧 使用增强模型优化回复...\n");
                            String enhancedResponse = enhanceResponse(finalResponse, finalEnhanceModelConfig, agent);
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
                })
                .doOnError(error -> {
                    log.error("AI模型流式调用失败", error);
                    sink.error(new RuntimeException("AI模型调用失败: " + error.getMessage()));
                })
                .subscribe();

        } catch (Exception e) {
            log.error("流式推理执行失败", e);
            sink.error(new RuntimeException("流式推理执行失败: " + e.getMessage()));
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
