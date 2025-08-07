package org.dromara.system.service;

import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatMessage;
import org.dromara.system.domain.dto.ChatRequestDto;

import java.util.List;

/**
 * 会话上下文管理服务接口
 *
 * @author zhoudashuai
 */
public interface ChatContextService {

    /**
     * 创建新会话
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @param title   会话标题
     * @return 会话信息
     */
    SysAgentChat createChat(Long agentId, Long userId, String title);

    /**
     * 获取会话信息
     *
     * @param chatId 会话ID
     * @return 会话信息
     */
    SysAgentChat getChatById(Long chatId);

    /**
     * 获取会话信息（通过UUID）
     *
     * @param chatUuid 会话UUID
     * @return 会话信息
     */
    SysAgentChat getChatByUuid(String chatUuid);

    /**
     * 更新会话信息
     *
     * @param chat 会话信息
     */
    void updateChat(SysAgentChat chat);

    /**
     * 获取会话历史消息
     *
     * @param chatId      会话ID
     * @param limit       限制数量（-1表示不限制）
     * @param includeSystem 是否包含系统消息
     * @return 消息列表
     */
    List<SysAgentChatMessage> getChatHistory(Long chatId, int limit, boolean includeSystem);

    /**
     * 获取会话上下文窗口内的消息
     *
     * @param chatId         会话ID
     * @param contextWindow  上下文窗口大小（token数）
     * @param includeSystem  是否包含系统消息
     * @return 消息列表
     */
    List<SysAgentChatMessage> getChatContextWindow(Long chatId, int contextWindow, boolean includeSystem);

    /**
     * 添加消息到会话
     *
     * @param message 消息信息
     * @return 保存后的消息
     */
    SysAgentChatMessage addMessage(SysAgentChatMessage message);

    /**
     * 批量添加消息
     *
     * @param messages 消息列表
     */
    void addMessages(List<SysAgentChatMessage> messages);

    /**
     * 更新消息
     *
     * @param message 消息信息
     */
    void updateMessage(SysAgentChatMessage message);


    /**
     * 清理会话上下文（根据token限制或消息数量）
     *
     * @param chatId     会话ID
     * @param maxTokens  最大token数
     * @param maxMessages 最大消息数
     */
    void cleanupContext(Long chatId, int maxTokens, int maxMessages);

    /**
     * 计算消息的token数
     *
     * @param content 消息内容
     * @return token数
     */
    int calculateTokens(String content);

    /**
     * 计算消息列表的总token数
     *
     * @param messages 消息列表
     * @return 总token数
     */
    int calculateTotalTokens(List<SysAgentChatMessage> messages);

    /**
     * 归档会话
     *
     * @param chatId 会话ID
     */
    void archiveChat(Long chatId);

    /**
     * 删除会话（逻辑删除）
     *
     * @param chatId 会话ID
     */
    void deleteChat(Long chatId);

    /**
     * 获取用户的会话列表
     *
     * @param userId  用户ID
     * @param agentId 智能体ID（可选）
     * @param status  状态（可选）
     * @param limit   限制数量
     * @return 会话列表
     */
    List<SysAgentChat> getUserChats(Long userId, Long agentId, String status, int limit);

    /**
     * 统计会话信息
     *
     * @param chatId 会话ID
     * @return 更新后的会话信息
     */
    SysAgentChat statisticsChat(Long chatId);

    /**
     * 创建会话快照（用于保存重要节点）
     *
     * @param chatId      会话ID
     * @param description 快照描述
     * @return 快照ID
     */
    Long createChatSnapshot(Long chatId, String description);

    /**
     * 恢复会话快照
     *
     * @param snapshotId 快照ID
     * @return 新的会话ID
     */
    Long restoreChatSnapshot(Long snapshotId);
}
