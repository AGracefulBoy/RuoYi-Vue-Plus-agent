package org.dromara.system.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.llm.model.protocol.resp.IChatResponse;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.domain.context.StreamingContext;
import org.dromara.system.domain.instruction.ToolCallInstruction;
import org.dromara.system.mapper.SysAgentChatMessageMapper;
import org.dromara.system.service.ChatContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息持久化辅助类
 * 负责处理聊天消息的保存和管理
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagePersistenceHelper {

    @Autowired
    private ChatContextService chatContextService;

    @Autowired
    private SysAgentChatMessageMapper agentChatMessageMapper;

    /**
     * 保存用户消息
     *
     * @param chatId    会话ID
     * @param userInput 用户输入
     */
    public void saveUserMessage(Long chatId, String userInput) {
        SysAgentChatMessage userMessage = new SysAgentChatMessage();
        userMessage.setChatId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(userInput);
        userMessage.setMessageType("text");
        userMessage.setStatus("completed");
        userMessage.setProcessingTime(0L);

        // 设置用户信息
        Long userId = LoginHelper.getUserId();
        if (userId != null) {
            userMessage.setCreateBy(userId);
            userMessage.setUpdateBy(userId);
            userMessage.setCreateDept(LoginHelper.getDeptId());
        }
        userMessage.setTenantId(TenantHelper.getTenantId());

        chatContextService.addMessage(userMessage);
    }

    /**
     * 保存用户消息（使用流式上下文）
     *
     * @param chatId    会话ID
     * @param userInput 用户输入
     * @param ctx       流式上下文
     */
    public void saveUserMessage(Long chatId, String userInput, StreamingContext ctx) {
        SysAgentChatMessage userMessage = new SysAgentChatMessage();
        userMessage.setChatId(chatId);
        userMessage.setRole("user");
        userMessage.setContent(userInput);
        userMessage.setMessageType("text");
        userMessage.setStatus("completed");
        userMessage.setMessageIndex(ctx.getAndIncrementMessageIndex());
        userMessage.setCreateBy(ctx.getUserId());
        userMessage.setUpdateBy(ctx.getUserId());
        userMessage.setTenantId(ctx.getTenantId());

        agentChatMessageMapper.insert(userMessage);
    }

    /**
     * 保存思考步骤
     *
     * @param chatId      会话ID
     * @param content     内容
     * @param messageType 消息类型
     * @param stepIndex   步骤索引
     * @param ctx         流式上下文
     */
    public void saveThoughtStep(Long chatId, String content, String messageType, int stepIndex, StreamingContext ctx) {
        if (chatId == null) return;

        // 当stepIndex==0且messageType为action时，剔除Action之前的内容
        String processedContent = content;
        if (stepIndex == 0 && "action".equals(messageType)) {
            // 查找"Action"关键字的位置
            int actionIndex = content.indexOf("Action");
            if (actionIndex >= 0) {
                // 只保留Action及之后的内容
                processedContent = content.substring(actionIndex);
            }
        }

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("assistant");
        message.setContent(processedContent);

        message.setMessageType(messageType);
        message.setStatus("completed");

        // 从StreamingContext获取用户信息，避免线程切换导致的上下文丢失
        Long userId = ctx.getUserId();
        if (userId != null) {
            message.setCreateBy(userId);
            message.setUpdateBy(userId);
            message.setCreateDept(ctx.getDeptId());
        }
        message.setTenantId(ctx.getTenantId());

        // 创建思维链步骤信息
        List<SysAgentChatMessage.ThoughtStep> thoughtSteps = new ArrayList<>();
        SysAgentChatMessage.ThoughtStep step = new SysAgentChatMessage.ThoughtStep();
        step.setStepType(messageType);
        step.setContent(processedContent);  // 使用处理后的内容
        step.setStepIndex(stepIndex);
        step.setSuccess(true);
        thoughtSteps.add(step);
        message.setThoughtSteps(thoughtSteps);

        chatContextService.addMessage(message);
    }

    /**
     * 保存工具执行结果
     *
     * @param chatId    会话ID
     * @param toolCall  工具调用指令
     * @param result    执行结果
     * @param stepIndex 步骤索引
     * @param ctx       流式上下文
     */
    public void saveToolResult(Long chatId, ToolCallInstruction toolCall, String result, int stepIndex, StreamingContext ctx) {
        if (chatId == null) return;

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("tool");
        message.setContent(result);
        message.setMessageType("action");
        message.setStatus("completed");

        // 从StreamingContext获取用户信息，避免线程切换导致的上下文丢失
        Long userId = ctx.getUserId();
        if (userId != null) {
            message.setCreateBy(userId);
            message.setUpdateBy(userId);
            message.setCreateDept(ctx.getDeptId());
        }
        message.setTenantId(ctx.getTenantId());

        // 创建工具调用信息
        List<SysAgentChatMessage.ToolCall> toolCalls = new ArrayList<>();
        SysAgentChatMessage.ToolCall call = new SysAgentChatMessage.ToolCall();
        call.setToolName(toolCall.getToolName());

        // 解析参数（假设是 JSON 格式）
        Map<String, Object> params = new HashMap<>();
        params.put("raw", toolCall.getParameters());
        call.setParameters(params);

        call.setResult(result);
        call.setSuccess(true);
        toolCalls.add(call);
        message.setToolCalls(toolCalls);

        chatContextService.addMessage(message);
    }

    /**
     * 保存最终答案到数据库
     *
     * @param chatId      会话ID
     * @param finalAnswer 最终答案
     * @param ctx         流式上下文
     * @param tokenUsage  Token使用情况
     */
    public void saveFinalAnswer(Long chatId, String finalAnswer, StreamingContext ctx, IChatResponse.Usage tokenUsage) {
        if (chatId == null || !StringUtils.hasText(finalAnswer)) return;

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("assistant");
        message.setContent(finalAnswer);
        message.setMessageType("answer");
        message.setStatus("completed");

        // 从StreamingContext获取用户信息，避免线程切换导致的上下文丢失
        Long userId = ctx.getUserId();
        if (userId != null) {
            message.setCreateBy(userId);
            message.setUpdateBy(userId);
            message.setCreateDept(ctx.getDeptId());
        }
        message.setTenantId(ctx.getTenantId());

        // 如果有token使用信息，添加到元数据
        if (tokenUsage != null) {
            SysAgentChatMessage.MessageMetadata metadata = new SysAgentChatMessage.MessageMetadata();
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("promptTokens", tokenUsage.getPromptTokens());
            tokenInfo.put("completionTokens", tokenUsage.getCompletionTokens());
            tokenInfo.put("totalTokens", tokenUsage.getTotalTokens());
            metadata.setTokenUsage(tokenInfo);
            message.setMetadata(metadata);
            message.setTokenCount(tokenUsage.getTotalTokens());
        }

        chatContextService.addMessage(message);
        log.info("保存最终答案到数据库，chatId: {}, 内容长度: {}", chatId, finalAnswer.length());
    }

    /**
     * 保存增强回复到数据库
     *
     * @param chatId          会话ID
     * @param enhancedContent 增强后的内容
     * @param ctx             流式上下文
     * @param tokenUsage      Token使用情况
     */
    public void saveEnhancedReply(Long chatId, String enhancedContent, StreamingContext ctx, IChatResponse.Usage tokenUsage) {
        if (chatId == null || !StringUtils.hasText(enhancedContent)) return;

        SysAgentChatMessage message = new SysAgentChatMessage();
        message.setChatId(chatId);
        message.setRole("assistant");
        message.setContent(enhancedContent);
        message.setMessageType("answer");
        message.setStatus("completed");

        // 从StreamingContext获取用户信息，避免线程切换导致的上下文丢失
        Long userId = ctx.getUserId();
        if (userId != null) {
            message.setCreateBy(userId);
            message.setUpdateBy(userId);
            message.setCreateDept(ctx.getDeptId());
        }
        message.setTenantId(ctx.getTenantId());

        // 如果有token使用信息，添加到元数据
        if (tokenUsage != null) {
            SysAgentChatMessage.MessageMetadata metadata = new SysAgentChatMessage.MessageMetadata();
            Map<String, Object> tokenInfo = new HashMap<>();
            tokenInfo.put("promptTokens", tokenUsage.getPromptTokens());
            tokenInfo.put("completionTokens", tokenUsage.getCompletionTokens());
            tokenInfo.put("totalTokens", tokenUsage.getTotalTokens());
            metadata.setTokenUsage(tokenInfo);
            message.setMetadata(metadata);
            message.setTokenCount(tokenUsage.getTotalTokens());
        }

        chatContextService.addMessage(message);
        log.info("保存增强回复到数据库，chatId: {}, 内容长度: {}", chatId, enhancedContent.length());
    }

    /**
     * 保存思考内容到数据库
     *
     * @param chatId  会话ID
     * @param content 思考内容
     * @param ctx     流式上下文
     */
    public void saveThoughtMessage(Long chatId, String content, StreamingContext ctx) {
        if (chatId == null || !StringUtils.hasText(content)) return;

        SysAgentChatMessage thoughtMessage = new SysAgentChatMessage();
        thoughtMessage.setChatId(chatId);
        thoughtMessage.setRole("assistant");
        thoughtMessage.setContent(content);
        thoughtMessage.setMessageType("thought");
        thoughtMessage.setStatus("completed");
        thoughtMessage.setMessageIndex(ctx.getAndIncrementMessageIndex());

        // 设置用户上下文信息
        thoughtMessage.setCreateBy(ctx.getUserId());
        thoughtMessage.setUpdateBy(ctx.getUserId());
        thoughtMessage.setTenantId(ctx.getTenantId());

        agentChatMessageMapper.insert(thoughtMessage);
        log.debug("保存thought内容到数据库 - chatId: {}, content length: {}", chatId, content.length());
    }

    /**
     * 保存助理回答到数据库
     *
     * @param chatId  会话ID
     * @param content 回答内容
     * @param ctx     流式上下文
     */
    public void saveAssistantAnswer(Long chatId, String content, StreamingContext ctx) {
        if (chatId == null || !StringUtils.hasText(content)) return;

        SysAgentChatMessage answerMessage = new SysAgentChatMessage();
        answerMessage.setChatId(chatId);
        answerMessage.setRole("assistant");
        answerMessage.setContent(content);
        answerMessage.setMessageType("answer");
        answerMessage.setStatus("completed");
        answerMessage.setMessageIndex(ctx.getAndIncrementMessageIndex());
        answerMessage.setCreateBy(ctx.getUserId());
        answerMessage.setUpdateBy(ctx.getUserId());
        answerMessage.setTenantId(ctx.getTenantId());

        agentChatMessageMapper.insert(answerMessage);
    }
}
