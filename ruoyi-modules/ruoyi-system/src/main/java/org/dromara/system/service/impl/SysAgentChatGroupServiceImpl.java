package org.dromara.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysAgentChat;
import org.dromara.system.domain.SysAgentChatGroup;
import org.dromara.system.domain.vo.ChatGroupTreeVo;
import org.dromara.system.domain.vo.ChatGroupVo;
import org.dromara.system.domain.vo.ChatSessionVo;
import org.dromara.system.mapper.SysAgentChatGroupMapper;
import org.dromara.system.mapper.SysAgentChatMapper;
import org.dromara.system.service.ChatContextService;
import org.dromara.system.service.ISysAgentChatGroupService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能体会话分组 业务层实现
 *
 * @author zhoudashuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAgentChatGroupServiceImpl implements ISysAgentChatGroupService {

    private final SysAgentChatGroupMapper groupMapper;
    private final SysAgentChatMapper chatMapper;
    private final ChatContextService chatContextService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChatGroup createGroup(SysAgentChatGroup group) {
        // 设置用户ID
        if (group.getUserId() == null) {
            group.setUserId(LoginHelper.getUserId());
        }

        // 检查分组名称是否重复
        if (checkGroupNameExists(group.getUserId(), group.getGroupName(), null)) {
            throw new ServiceException("分组名称已存在");
        }

        // 设置默认值
        if (group.getSortOrder() == null) {
            group.setSortOrder(0);
        }
        if (StrUtil.isBlank(group.getIsDefault())) {
            group.setIsDefault("0");
        }

        // 插入分组
        groupMapper.insert(group);

        log.info("创建分组成功，分组ID: {}, 分组名称: {}, 用户ID: {}",
            group.getGroupId(), group.getGroupName(), group.getUserId());

        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroup(SysAgentChatGroup group) {
        // 验证分组是否存在
        SysAgentChatGroup existingGroup = groupMapper.selectById(group.getGroupId());
        if (existingGroup == null) {
            throw new ServiceException("分组不存在");
        }

        // 验证权限
        if (!existingGroup.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("无权修改此分组");
        }

        // 检查分组名称是否重复
        if (StrUtil.isNotBlank(group.getGroupName()) &&
            checkGroupNameExists(existingGroup.getUserId(), group.getGroupName(), group.getGroupId())) {
            throw new ServiceException("分组名称已存在");
        }

        // 更新分组
        int result = groupMapper.updateById(group);

        log.info("更新分组，分组ID: {}, 结果: {}", group.getGroupId(), result > 0);

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGroup(Long groupId, Long moveToGroup) {
        // 验证分组是否存在
        SysAgentChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new ServiceException("分组不存在");
        }

        // 验证权限
        if (!group.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("无权删除此分组");
        }

        // 不允许删除默认分组
        if ("1".equals(group.getIsDefault())) {
            throw new ServiceException("不能删除默认分组");
        }

        // 处理分组下的会话
        LambdaUpdateWrapper<SysAgentChat> chatWrapper = new LambdaUpdateWrapper<>();
        chatWrapper.eq(SysAgentChat::getGroupId, groupId);

        if (moveToGroup != null) {
            // 验证目标分组是否存在
            SysAgentChatGroup targetGroup = groupMapper.selectById(moveToGroup);
            if (targetGroup == null || !targetGroup.getUserId().equals(group.getUserId())) {
                throw new ServiceException("目标分组不存在");
            }
            // 移动会话到目标分组
            chatWrapper.set(SysAgentChat::getGroupId, moveToGroup);
        } else {
            // 获取或创建默认分组
            SysAgentChatGroup defaultGroup = getOrCreateDefaultGroup(group.getUserId());
            chatWrapper.set(SysAgentChat::getGroupId, defaultGroup.getGroupId());
        }

        chatMapper.update(null, chatWrapper);

        // 处理子分组（如果有）
        LambdaUpdateWrapper<SysAgentChatGroup> subGroupWrapper = new LambdaUpdateWrapper<>();
        subGroupWrapper.eq(SysAgentChatGroup::getParentId, groupId)
            .set(SysAgentChatGroup::getParentId, null);
        groupMapper.update(null, subGroupWrapper);

        // 删除分组
        int result = groupMapper.deleteById(groupId);

        log.info("删除分组，分组ID: {}, 移动到分组: {}, 结果: {}",
            groupId, moveToGroup, result > 0);

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGroups(List<Long> groupIds) {
        if (CollUtil.isEmpty(groupIds)) {
            return true;
        }

        Long userId = LoginHelper.getUserId();

        // 验证所有分组的权限
        List<SysAgentChatGroup> groups = groupMapper.selectBatchIds(groupIds);
        for (SysAgentChatGroup group : groups) {
            if (!group.getUserId().equals(userId)) {
                throw new ServiceException("无权删除分组: " + group.getGroupName());
            }
            if ("1".equals(group.getIsDefault())) {
                throw new ServiceException("不能删除默认分组: " + group.getGroupName());
            }
        }

        // 获取默认分组
        SysAgentChatGroup defaultGroup = getOrCreateDefaultGroup(userId);

        // 将所有会话移动到默认分组
        LambdaUpdateWrapper<SysAgentChat> chatWrapper = new LambdaUpdateWrapper<>();
        chatWrapper.in(SysAgentChat::getGroupId, groupIds)
            .set(SysAgentChat::getGroupId, defaultGroup.getGroupId());
        chatMapper.update(null, chatWrapper);

        // 批量删除分组
        int result = groupMapper.deleteByIds(groupIds);

        log.info("批量删除分组，数量: {}, 结果: {}", groupIds.size(), result);

        return result > 0;
    }

    @Override
    public ChatGroupVo getGroupById(Long groupId) {
        SysAgentChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            return null;
        }

        // 验证权限
        if (!group.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("无权查看此分组");
        }

        return groupMapper.selectVoById(groupId);
    }

    @Override
    public List<ChatGroupVo> getUserGroups(Long userId) {
        if (userId == null) {
            userId = LoginHelper.getUserId();
        }

        // 获取分组列表（包含会话数量）
        return groupMapper.selectGroupsWithChatCount(userId);
    }

    @Override
    public List<ChatGroupTreeVo> getUserGroupTree(Long userId, boolean includeChats) {
        if (userId == null) {
            userId = LoginHelper.getUserId();
        }

        // 获取所有分组
        List<ChatGroupVo> groups = getUserGroups(userId);

        // 转换为树形结构
        Map<Long, ChatGroupTreeVo> groupMap = new HashMap<>();
        List<ChatGroupTreeVo> rootGroups = new ArrayList<>();

        // 第一遍：创建所有节点
        for (ChatGroupVo group : groups) {
            ChatGroupTreeVo treeVo = new ChatGroupTreeVo();
            BeanUtils.copyProperties(group, treeVo);
            treeVo.setChildren(new ArrayList<>());
            treeVo.setExpanded(false);
            groupMap.put(group.getGroupId(), treeVo);
        }

        // 第二遍：构建树形结构
        for (ChatGroupVo group : groups) {
            ChatGroupTreeVo treeVo = groupMap.get(group.getGroupId());
            if (group.getParentId() == null) {
                rootGroups.add(treeVo);
            } else {
                ChatGroupTreeVo parent = groupMap.get(group.getParentId());
                if (parent != null) {
                    parent.getChildren().add(treeVo);
                    parent.setHasChildren(true);
                }
            }
        }

        // 如果需要包含会话列表
        if (includeChats) {
            List<SysAgentChat> chats = chatContextService.getUserChats(userId, null, "active", -1);
            Map<Long, List<ChatSessionVo>> chatsByGroup = new HashMap<>();

            for (SysAgentChat chat : chats) {
                ChatSessionVo sessionVo = new ChatSessionVo();
                sessionVo.setChatId(chat.getChatId());
                sessionVo.setConversationId(chat.getConversationId());
                sessionVo.setAgentId(chat.getAgentId());
                sessionVo.setGroupId(chat.getGroupId());
                sessionVo.setTitle(chat.getChatTitle());
                sessionVo.setStatus(chat.getStatus());
                sessionVo.setMessageCount(chat.getMessageCount());
                sessionVo.setLastActiveTime(chat.getLastActiveTime());
                sessionVo.setCreatedTime(chat.getCreateTime());

                Long groupId = chat.getGroupId();
                if (groupId != null) {
                    chatsByGroup.computeIfAbsent(groupId, k -> new ArrayList<>()).add(sessionVo);
                }
            }

            // 将会话分配到对应的分组
            for (ChatGroupTreeVo group : groupMap.values()) {
                List<ChatSessionVo> groupChats = chatsByGroup.get(group.getGroupId());
                if (groupChats != null) {
                    group.setChats(groupChats);
                } else {
                    group.setChats(new ArrayList<>());
                }
            }
        }

        // 排序
        sortGroupTree(rootGroups);

        return rootGroups;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysAgentChatGroup getOrCreateDefaultGroup(Long userId) {
        if (userId == null) {
            userId = LoginHelper.getUserId();
        }

        // 查找默认分组
        SysAgentChatGroup defaultGroup = groupMapper.selectDefaultGroup(userId);

        if (defaultGroup == null) {
            // 创建默认分组
            defaultGroup = new SysAgentChatGroup();
            defaultGroup.setUserId(userId);
            defaultGroup.setGroupName("未分组");
            defaultGroup.setIsDefault("1");
            defaultGroup.setSortOrder(0);
            defaultGroup.setIcon("folder");
            defaultGroup.setColor("#999999");

            groupMapper.insert(defaultGroup);

            log.info("为用户创建默认分组，用户ID: {}, 分组ID: {}",
                userId, defaultGroup.getGroupId());
        }

        return defaultGroup;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveChatToGroup(Long chatId, Long groupId) {
        // 验证会话
        SysAgentChat chat = chatMapper.selectById(chatId);
        if (chat == null) {
            throw new ServiceException("会话不存在");
        }

        // 验证权限
        if (!chat.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("无权操作此会话");
        }

        // 验证目标分组
        if (groupId != null) {
            SysAgentChatGroup group = groupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(chat.getUserId())) {
                throw new ServiceException("目标分组不存在");
            }
        }

        // 更新会话分组
        chat.setGroupId(groupId);
        int result = chatMapper.updateById(chat);

        log.info("移动会话到分组，会话ID: {}, 分组ID: {}, 结果: {}",
            chatId, groupId, result > 0);

        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean moveChatsToGroup(List<Long> chatIds, Long groupId) {
        if (CollUtil.isEmpty(chatIds)) {
            return true;
        }

        Long userId = LoginHelper.getUserId();

        // 验证目标分组
        if (groupId != null) {
            SysAgentChatGroup group = groupMapper.selectById(groupId);
            if (group == null || !group.getUserId().equals(userId)) {
                throw new ServiceException("目标分组不存在");
            }
        }

        // 批量更新会话分组
        LambdaUpdateWrapper<SysAgentChat> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(SysAgentChat::getChatId, chatIds)
            .eq(SysAgentChat::getUserId, userId)
            .set(SysAgentChat::getGroupId, groupId);

        int result = chatMapper.update(null, wrapper);

        log.info("批量移动会话到分组，会话数量: {}, 分组ID: {}, 结果: {}",
            chatIds.size(), groupId, result);

        return result > 0;
    }

    @Override
    public boolean checkGroupNameExists(Long userId, String groupName, Long excludeId) {
        if (StrUtil.isBlank(groupName)) {
            return false;
        }

        LambdaQueryWrapper<SysAgentChatGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysAgentChatGroup::getUserId, userId)
            .eq(SysAgentChatGroup::getGroupName, groupName);

        if (excludeId != null) {
            wrapper.ne(SysAgentChatGroup::getGroupId, excludeId);
        }

        return groupMapper.selectCount(wrapper) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroupSort(Long groupId, Integer sortOrder) {
        SysAgentChatGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new ServiceException("分组不存在");
        }

        // 验证权限
        if (!group.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("无权操作此分组");
        }

        group.setSortOrder(sortOrder);
        return groupMapper.updateById(group) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroupsSort(Map<Long, Integer> sortMap) {
        if (CollUtil.isEmpty(sortMap)) {
            return true;
        }

        Long userId = LoginHelper.getUserId();
        List<SysAgentChatGroup> groups = groupMapper.selectBatchIds(sortMap.keySet());

        // 验证权限并更新排序
        for (SysAgentChatGroup group : groups) {
            if (!group.getUserId().equals(userId)) {
                throw new ServiceException("无权操作分组: " + group.getGroupName());
            }
            Integer sortOrder = sortMap.get(group.getGroupId());
            if (sortOrder != null) {
                group.setSortOrder(sortOrder);
            }
        }

        // 批量更新
        return groupMapper.updateBatchById(groups);
    }

    /**
     * 递归排序分组树
     */
    private void sortGroupTree(List<ChatGroupTreeVo> groups) {
        if (CollUtil.isEmpty(groups)) {
            return;
        }

        // 按排序值和创建时间排序
        groups.sort((a, b) -> {
            int sortCompare = Integer.compare(
                a.getSortOrder() != null ? a.getSortOrder() : 0,
                b.getSortOrder() != null ? b.getSortOrder() : 0
            );
            if (sortCompare != 0) {
                return sortCompare;
            }
            if (a.getCreateTime() != null && b.getCreateTime() != null) {
                return a.getCreateTime().compareTo(b.getCreateTime());
            }
            return 0;
        });

        // 递归排序子分组
        for (ChatGroupTreeVo group : groups) {
            if (CollUtil.isNotEmpty(group.getChildren())) {
                sortGroupTree(group.getChildren());
            }
        }
    }
}
