package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.mapper.SysAgentChatMapper;
import org.dromara.system.mapper.SysAgentChatMessageMapper;
import org.dromara.system.service.ChatContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
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
        SysAgentChat chat = new SysAgentChat();
        chat.setChatUuid(IdUtil.fastSimpleUUID());
        chat.setAgentId(agentId);
        chat.setUserId(userId);
        chat.setChatTitle(StrUtil.blankToDefault(title, "新会话"));
        chat.setStatus("active");
        chat.setMessageCount(0);
        chat.setTotalTokens(0);
        chat.setInputTokens(0);
        chat.setOutputTokens(0);
        chat.setStartTime(new Date());
        chat.setLastActiveTime(new Date());

        chatMapper.insert(chat);

        // 缓存会话信息
        cacheChat(chat);

        log.info("创建新会话成功，会话ID: {}, 用户ID: {}, 智能体ID: {}",
            chat.getChatId(), userId, agentId);

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
    public List<SysAgentChatMessage> getChatContextWindow(Long chatId, int contextWindow, boolean includeSystem) {
        // 获取所有消息
        List<SysAgentChatMessage> allMessages = getChatHistory(chatId, -1, includeSystem);

        if (CollUtil.isEmpty(allMessages)) {
            return new ArrayList<>();
        }

        // 从后往前累计token，直到达到上下文窗口限制
        List<SysAgentChatMessage> contextMessages = new ArrayList<>();
        int totalTokens = 0;

        for (int i = allMessages.size() - 1; i >= 0; i--) {
            SysAgentChatMessage message = allMessages.get(i);
            int messageTokens = message.getTokenCount() != null ? message.getTokenCount() :
                calculateTokens(message.getContent());

            if (totalTokens + messageTokens > contextWindow && !contextMessages.isEmpty()) {
                break;
            }

            contextMessages.add(0, message);
            totalTokens += messageTokens;
        }

        return contextMessages;
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

        message.setCreateTime(new Date());
        messageMapper.insert(message);

        // 更新会话统计信息
        updateChatStatistics(message.getChatId());

        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addMessages(List<SysAgentChatMessage> messages) {
        if (CollUtil.isEmpty(messages)) {
            return;
        }

        Long chatId = messages.get(0).getChatId();
        Integer maxIndex = getMaxMessageIndex(chatId);

        for (int i = 0; i < messages.size(); i++) {
            SysAgentChatMessage message = messages.get(i);
            message.setMessageIndex(maxIndex + i + 1);

            if (message.getTokenCount() == null) {
                message.setTokenCount(calculateTokens(message.getContent()));
            }

            if (StrUtil.isBlank(message.getStatus())) {
                message.setStatus("completed");
            }

            message.setCreateTime(new Date());
        }

        // 批量插入
        messageMapper.insertBatch(messages);

        // 更新会话统计
        updateChatStatistics(chatId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMessage(SysAgentChatMessage message) {
        message.setUpdateTime(new Date());
        messageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanupContext(Long chatId, int maxTokens, int maxMessages) {
        List<SysAgentChatMessage> messages = getChatHistory(chatId, -1, true);

        if (CollUtil.isEmpty(messages)) {
            return;
        }

        // 需要删除的消息ID列表
        List<Long> toDelete = new ArrayList<>();
        int totalTokens = 0;
        int messageCount = 0;

        // 从后往前遍历，保留最新的消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            SysAgentChatMessage message = messages.get(i);
            totalTokens += message.getTokenCount();
            messageCount++;

            if (totalTokens > maxTokens || messageCount > maxMessages) {
                // 标记前面的消息为删除
                for (int j = 0; j <= i; j++) {
                    toDelete.add(messages.get(j).getMessageId());
                }
                break;
            }
        }

        if (CollUtil.isNotEmpty(toDelete)) {
            // 逻辑删除旧消息
            LambdaUpdateWrapper<SysAgentChatMessage> wrapper = new LambdaUpdateWrapper<>();
            wrapper.in(SysAgentChatMessage::getMessageId, toDelete)
                .set(SysAgentChatMessage::getDelFlag, "2");

            messageMapper.update(null, wrapper);

            log.info("清理会话上下文，会话ID: {}, 删除消息数: {}", chatId, toDelete.size());
        }
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
    public int calculateTotalTokens(List<SysAgentChatMessage> messages) {
        if (CollUtil.isEmpty(messages)) {
            return 0;
        }

        return messages.stream()
            .mapToInt(msg -> msg.getTokenCount() != null ? msg.getTokenCount() : 0)
            .sum();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveChat(Long chatId) {
        SysAgentChat chat = getChatById(chatId);
        if (chat == null) {
            throw new ServiceException("会话不存在");
        }

        chat.setStatus("archived");
        chat.setEndTime(new Date());
        updateChat(chat);

        // 清除缓存
        clearChatCache(chatId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChat(Long chatId) {
        // 逻辑删除会话
        LambdaUpdateWrapper<SysAgentChat> chatWrapper = new LambdaUpdateWrapper<>();
        chatWrapper.eq(SysAgentChat::getChatId, chatId)
            .set(SysAgentChat::getDelFlag, "2");

        chatMapper.update(null, chatWrapper);

        // 逻辑删除所有消息
        LambdaUpdateWrapper<SysAgentChatMessage> messageWrapper = new LambdaUpdateWrapper<>();
        messageWrapper.eq(SysAgentChatMessage::getChatId, chatId)
            .set(SysAgentChatMessage::getDelFlag, "2");

        messageMapper.update(null, messageWrapper);

        // 清除缓存
        clearChatCache(chatId);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createChatSnapshot(Long chatId, String description) {
        // TODO: 实现会话快照功能
        throw new ServiceException("会话快照功能暂未实现");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long restoreChatSnapshot(Long snapshotId) {
        // TODO: 实现会话快照恢复功能
        throw new ServiceException("会话快照恢复功能暂未实现");
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
