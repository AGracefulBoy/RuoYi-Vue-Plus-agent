package org.dromara.system.service;

import org.dromara.system.domain.SysAgentChatGroup;
import org.dromara.system.domain.vo.ChatGroupTreeVo;
import org.dromara.system.domain.vo.ChatGroupVo;

import java.util.List;

/**
 * 智能体会话分组 业务层
 *
 * @author zhoudashuai
 */
public interface ISysAgentChatGroupService {

    /**
     * 创建分组
     *
     * @param group 分组信息
     * @return 创建结果
     */
    SysAgentChatGroup createGroup(SysAgentChatGroup group);

    /**
     * 更新分组
     *
     * @param group 分组信息
     * @return 更新结果
     */
    boolean updateGroup(SysAgentChatGroup group);

    /**
     * 删除分组
     *
     * @param groupId    分组ID
     * @param moveToGroup 将分组下的会话移动到指定分组（为null则删除会话）
     * @return 删除结果
     */
    boolean deleteGroup(Long groupId, Long moveToGroup);

    /**
     * 批量删除分组
     *
     * @param groupIds 分组ID列表
     * @return 删除结果
     */
    boolean deleteGroups(List<Long> groupIds);

    /**
     * 获取分组详情
     *
     * @param groupId 分组ID
     * @return 分组信息
     */
    ChatGroupVo getGroupById(Long groupId);

    /**
     * 获取用户的分组列表
     *
     * @param userId 用户ID
     * @return 分组列表
     */
    List<ChatGroupVo> getUserGroups(Long userId);

    /**
     * 获取用户的分组树形结构
     *
     * @param userId        用户ID
     * @param includeChats  是否包含会话列表
     * @return 分组树形结构
     */
    List<ChatGroupTreeVo> getUserGroupTree(Long userId, boolean includeChats);

    /**
     * 获取或创建用户的默认分组
     *
     * @param userId 用户ID
     * @return 默认分组
     */
    SysAgentChatGroup getOrCreateDefaultGroup(Long userId);

    /**
     * 移动会话到分组
     *
     * @param chatId  会话ID
     * @param groupId 目标分组ID
     * @return 移动结果
     */
    boolean moveChatToGroup(Long chatId, Long groupId);

    /**
     * 批量移动会话到分组
     *
     * @param chatIds 会话ID列表
     * @param groupId 目标分组ID
     * @return 移动结果
     */
    boolean moveChatsToGroup(List<Long> chatIds, Long groupId);

    /**
     * 检查分组名称是否存在
     *
     * @param userId    用户ID
     * @param groupName 分组名称
     * @param excludeId 排除的分组ID（用于编辑时）
     * @return 是否存在
     */
    boolean checkGroupNameExists(Long userId, String groupName, Long excludeId);

    /**
     * 更新分组排序
     *
     * @param groupId   分组ID
     * @param sortOrder 排序值
     * @return 更新结果
     */
    boolean updateGroupSort(Long groupId, Integer sortOrder);

    /**
     * 批量更新分组排序
     *
     * @param sortMap 分组ID和排序值的映射
     * @return 更新结果
     */
    boolean updateGroupsSort(java.util.Map<Long, Integer> sortMap);
}