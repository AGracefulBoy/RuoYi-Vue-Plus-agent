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
     * 创建新会话（指定分组）
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @param title   会话标题
     * @param groupId 分组ID
     * @return 会话信息
     */
    SysAgentChat createChat(Long agentId, Long userId, String title, Long groupId);

    /**
     * 创建新会话（指定分组和对话模式）
     *
     * @param agentId   智能体ID
     * @param userId    用户ID
     * @param title     会话标题
     * @param groupId   分组ID
     * @param chatModel 对话模式（debug 表示调试模式，chat 表示正常对话）
     * @return 会话信息
     */
    SysAgentChat createChat(Long agentId, Long userId, String title, Long groupId, String chatModel);

    /**
     * 获取会话信息
     *
     * @param chatId 会话ID
     * @return 会话信息
     */
    SysAgentChat getChatById(Long chatId);

    /**
     * 获取用户最新的debug模式会话
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @return 会话信息，如果不存在返回null
     */
    SysAgentChat getLatestDebugChat(Long agentId, Long userId);

    /**
     * 根据分组ID获取会话列表
     *
     * @param groupId 分组ID
     * @param limit   限制数量（-1表示不限制）
     * @return 会话列表
     */
    List<SysAgentChat> getChatsByGroup(Long groupId, int limit);

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
     * 添加消息到会话
     *
     * @param message 消息信息
     * @return 保存后的消息
     */
    SysAgentChatMessage addMessage(SysAgentChatMessage message);



    /**
     * 计算消息的token数
     *
     * @param content 消息内容
     * @return token数
     */
    int calculateTokens(String content);


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
}
