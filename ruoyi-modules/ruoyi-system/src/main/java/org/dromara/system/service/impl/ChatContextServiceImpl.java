package org.dromara.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.mapper.SysAgentChatMapper;
import org.dromara.system.mapper.SysAgentChatMessageMapper;
import org.dromara.system.service.ChatContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;

/**
 * 会话上下文管理服务实现
 *
 * @author zhoudashuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextServiceImpl implements ChatContextService {

    private final SysAgentChatMapper chatMapper;
    private final SysAgentChatMessageMapper messageMapper;

    private static final String CHAT_CACHE_KEY = "chat:context:";
    private static final Duration CHAT_CACHE_TTL = Duration.ofHours(2);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChat createChat(Long agentId, Long userId, String title) {
        return createChat(agentId, userId, title, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChat createChat(Long agentId, Long userId, String title, Long groupId) {
        return createChat(agentId, userId, title, groupId, "chat");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChat createChat(Long agentId, Long userId, String title, Long groupId, String chatModel) {
        SysAgentChat chat = new SysAgentChat();
        chat.setChatUuid(IdUtil.fastSimpleUUID());
        chat.setAgentId(agentId);
        chat.setUserId(userId);
        chat.setGroupId(groupId);
        chat.setChatTitle(StrUtil.blankToDefault(title, "新会话"));
        chat.setStatus("active");
        chat.setMessageCount(0);
        chat.setTotalTokens(0);
        chat.setInputTokens(0);
        chat.setOutputTokens(0);
        chat.setStartTime(new Date());
        chat.setLastActiveTime(new Date());
        chat.setChatModel(StrUtil.blankToDefault(chatModel, "chat"));

        chatMapper.insert(chat);

        // 缓存会话信息
        cacheChat(chat);

        log.info("创建新会话成功，会话ID: {}, 用户ID: {}, 智能体ID: {}, 分组ID: {}, 对话模式: {}",
            chat.getChatId(), userId, agentId, groupId, chat.getChatModel());

        return chat;
    }

    @Override
    public SysAgentChat getChatById(Long chatId) {
        // 先从缓存获取
        String cacheKey = CHAT_CACHE_KEY + chatId;
        SysAgentChat chat = RedisUtils.getCacheObject(cacheKey);

        if (chat == null) {
            chat = chatMapper.selectById(chatId);
            if (chat != null) {
                cacheChat(chat);
            }
        }

        return chat;
    }

    @Override
    public SysAgentChat getChatByUuid(String chatUuid) {
        LambdaQueryWrapper<SysAgentChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChat::getChatUuid, chatUuid);
        return chatMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateChat(SysAgentChat chat) {
        chat.setUpdateTime(new Date());
        chatMapper.updateById(chat);

        // 更新缓存
        cacheChat(chat);
    }

    @Override
    public List<SysAgentChatMessage> getChatHistory(Long chatId, int limit, boolean includeSystem) {
        LambdaQueryWrapper<SysAgentChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChatMessage::getChatId, chatId);

        if (!includeSystem) {
            wrapper.ne(SysAgentChatMessage::getRole, "system");
        }

        wrapper.orderByAsc(SysAgentChatMessage::getMessageIndex);

        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        return messageMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChatMessage addMessage(SysAgentChatMessage message) {
        // 设置消息序号
        Integer maxIndex = getMaxMessageIndex(message.getChatId());
        message.setMessageIndex(maxIndex + 1);

        // 计算token数
        if (message.getTokenCount() == null) {
            message.setTokenCount(calculateTokens(message.getContent()));
        }

        // 设置默认状态
        if (StrUtil.isBlank(message.getStatus())) {
            message.setStatus("completed");
        }

        // createTime将由MyBatis-Plus的InjectionMetaObjectHandler自动填充
        messageMapper.insert(message);

        // 更新会话统计信息
        updateChatStatistics(message.getChatId());

        return message;
    }


    @Override
    public int calculateTokens(String content) {
        if (StrUtil.isBlank(content)) {
            return 0;
        }

        // 简单的token估算：中文约1.5个字符一个token，英文约4个字符一个token
        // 实际项目中应该使用tiktoken或其他准确的tokenizer
        int chineseCount = 0;
        int englishCount = 0;

        for (char c : content.toCharArray()) {
            if (c >= 0x4e00 && c <= 0x9fa5) {
                chineseCount++;
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                englishCount++;
            }
        }

        return (int) (chineseCount / 1.5 + englishCount / 4.0 + (content.length() - chineseCount - englishCount) / 3.0);
    }

    @Override
    public List<SysAgentChat> getUserChats(Long userId, Long agentId, String status, int limit) {
        LambdaQueryWrapper<SysAgentChat> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChat::getUserId, userId);

        if (agentId != null) {
            wrapper.eq(SysAgentChat::getAgentId, agentId);
        }

        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(SysAgentChat::getStatus, status);
        }

        wrapper.orderByDesc(SysAgentChat::getLastActiveTime);

        if (limit > 0) {
            wrapper.last("LIMIT " + limit);
        }

        return chatMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChat statisticsChat(Long chatId) {
        // 统计消息数量和token使用
        LambdaQueryWrapper<SysAgentChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChatMessage::getChatId, chatId);

        List<SysAgentChatMessage> messages = messageMapper.selectList(wrapper);

        int messageCount = messages.size();
        int inputTokens = 0;
        int outputTokens = 0;

        for (SysAgentChatMessage message : messages) {
            int tokens = message.getTokenCount() != null ? message.getTokenCount() : 0;

            if ("user".equals(message.getRole())) {
                inputTokens += tokens;
            } else {
                outputTokens += tokens;
            }
        }

        // 更新会话统计
        SysAgentChat chat = getChatById(chatId);
        if (chat != null) {
            chat.setMessageCount(messageCount);
            chat.setInputTokens(inputTokens);
            chat.setOutputTokens(outputTokens);
            chat.setTotalTokens(inputTokens + outputTokens);
            chat.setLastActiveTime(new Date());

            updateChat(chat);
        }

        return chat;
    }

    /**
     * 缓存会话信息
     */
    private void cacheChat(SysAgentChat chat) {
        String cacheKey = CHAT_CACHE_KEY + chat.getChatId();
        RedisUtils.setCacheObject(cacheKey, chat, CHAT_CACHE_TTL);
    }

    /**
     * 清除会话缓存
     */
    private void clearChatCache(Long chatId) {
        String cacheKey = CHAT_CACHE_KEY + chatId;
        RedisUtils.deleteObject(cacheKey);
    }

    /**
     * 获取会话最大消息序号
     */
    private Integer getMaxMessageIndex(Long chatId) {
        LambdaQueryWrapper<SysAgentChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChatMessage::getChatId, chatId)
            .orderByDesc(SysAgentChatMessage::getMessageIndex)
            .last("LIMIT 1");

        SysAgentChatMessage lastMessage = messageMapper.selectOne(wrapper);
        return lastMessage != null ? lastMessage.getMessageIndex() : 0;
    }

    /**
     * 更新会话统计信息
     */
    private void updateChatStatistics(Long chatId) {
        statisticsChat(chatId);
    }
}
