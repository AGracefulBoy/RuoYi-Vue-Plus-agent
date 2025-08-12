package org.dromara.system.controller.core;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysAgentChatGroup;
import org.dromara.system.domain.vo.ChatGroupTreeVo;
import org.dromara.system.domain.vo.ChatGroupVo;
import org.dromara.system.service.ISysAgentChatGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 智能体会话分组管理接口
 *
 * @author zhoudashuai
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/chat/v1/groups")
public class SysAgentChatGroupController {

    private final ISysAgentChatGroupService chatGroupService;

    /**
     * 创建分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.INSERT)
    @PostMapping
    public R<ChatGroupVo> createGroup(@Valid @RequestBody SysAgentChatGroup group) {
        Long userId = LoginHelper.getUserId();
        group.setUserId(userId);

        SysAgentChatGroup createdGroup = chatGroupService.createGroup(group);

        ChatGroupVo vo = new ChatGroupVo();
        vo.setGroupId(createdGroup.getGroupId());
        vo.setUserId(createdGroup.getUserId());
        vo.setGroupName(createdGroup.getGroupName());
        vo.setParentId(createdGroup.getParentId());
        vo.setIcon(createdGroup.getIcon());
        vo.setColor(createdGroup.getColor());
        vo.setSortOrder(createdGroup.getSortOrder());
        vo.setIsDefault(createdGroup.getIsDefault());
        vo.setMetadata(createdGroup.getMetadata());
        vo.setCreateTime(createdGroup.getCreateTime());
        vo.setUpdateTime(createdGroup.getUpdateTime());

        return R.ok(vo);
    }

    /**
     * 更新分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.UPDATE)
    @PutMapping("/{groupId}")
    public R<Void> updateGroup(@PathVariable @NotNull(message = "分组ID不能为空") Long groupId,
                               @Valid @RequestBody SysAgentChatGroup group) {
        group.setGroupId(groupId);
        boolean result = chatGroupService.updateGroup(group);
        return result ? R.ok() : R.fail("更新分组失败");
    }

    /**
     * 删除分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.DELETE)
    @DeleteMapping("/{groupId}")
    public R<Void> deleteGroup(@PathVariable @NotNull(message = "分组ID不能为空") Long groupId,
                               @RequestParam(required = false) Long moveToGroup) {
        boolean result = chatGroupService.deleteGroup(groupId, moveToGroup);
        return result ? R.ok() : R.fail("删除分组失败");
    }

    /**
     * 批量删除分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.DELETE)
    @DeleteMapping
    public R<Void> deleteGroups(@RequestBody List<Long> groupIds) {
        boolean result = chatGroupService.deleteGroups(groupIds);
        return result ? R.ok() : R.fail("批量删除分组失败");
    }

    /**
     * 获取分组详情
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping("/{groupId}")
    public R<ChatGroupVo> getGroup(@PathVariable @NotNull(message = "分组ID不能为空") Long groupId) {
        ChatGroupVo group = chatGroupService.getGroupById(groupId);
        return R.ok(group);
    }

    /**
     * 获取用户的分组列表
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping
    public R<List<ChatGroupVo>> getUserGroups() {
        Long userId = LoginHelper.getUserId();
        List<ChatGroupVo> groups = chatGroupService.getUserGroups(userId);
        return R.ok(groups);
    }

    /**
     * 获取用户的分组树形结构
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping("/tree")
    public R<List<ChatGroupTreeVo>> getUserGroupTree(@RequestParam(defaultValue = "false") boolean includeChats) {
        Long userId = LoginHelper.getUserId();
        List<ChatGroupTreeVo> tree = chatGroupService.getUserGroupTree(userId, includeChats);
        return R.ok(tree);
    }

    /**
     * 移动会话到分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.UPDATE)
    @PutMapping("/chat/{chatId}")
    public R<Void> moveChatToGroup(@PathVariable @NotNull(message = "会话ID不能为空") Long chatId,
                                   @RequestParam @NotNull(message = "分组ID不能为空") Long groupId) {
        boolean result = chatGroupService.moveChatToGroup(chatId, groupId);
        return result ? R.ok() : R.fail("移动会话失败");
    }

    /**
     * 批量移动会话到分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.UPDATE)
    @PutMapping("/chats")
    public R<Void> moveChatsToGroup(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Long> chatIds = (List<Long>) params.get("chatIds");
        Long groupId = Long.valueOf(params.get("groupId").toString());

        boolean result = chatGroupService.moveChatsToGroup(chatIds, groupId);
        return result ? R.ok() : R.fail("批量移动会话失败");
    }

    /**
     * 检查分组名称是否存在
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @GetMapping("/check-name")
    public R<Boolean> checkGroupName(@RequestParam @NotNull(message = "分组名称不能为空") String groupName,
                                     @RequestParam(required = false) Long excludeId) {
        Long userId = LoginHelper.getUserId();
        boolean exists = chatGroupService.checkGroupNameExists(userId, groupName, excludeId);
        return R.ok(exists);
    }

    /**
     * 更新分组排序
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.UPDATE)
    @PutMapping("/{groupId}/sort")
    public R<Void> updateGroupSort(@PathVariable @NotNull(message = "分组ID不能为空") Long groupId,
                                   @RequestParam @NotNull(message = "排序值不能为空") Integer sortOrder) {
        boolean result = chatGroupService.updateGroupSort(groupId, sortOrder);
        return result ? R.ok() : R.fail("更新排序失败");
    }

    /**
     * 批量更新分组排序
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @Log(title = "会话分组", businessType = BusinessType.UPDATE)
    @PutMapping("/sort")
    public R<Void> updateGroupsSort(@RequestBody Map<Long, Integer> sortMap) {
        boolean result = chatGroupService.updateGroupsSort(sortMap);
        return result ? R.ok() : R.fail("批量更新排序失败");
    }

    /**
     * 获取或创建默认分组
     */
    @SaCheckPermission("system:agent:list")
    @SaCheckLogin
    @PostMapping("/default")
    public R<ChatGroupVo> getOrCreateDefaultGroup() {
        Long userId = LoginHelper.getUserId();
        SysAgentChatGroup defaultGroup = chatGroupService.getOrCreateDefaultGroup(userId);

        ChatGroupVo vo = new ChatGroupVo();
        vo.setGroupId(defaultGroup.getGroupId());
        vo.setUserId(defaultGroup.getUserId());
        vo.setGroupName(defaultGroup.getGroupName());
        vo.setIsDefault(defaultGroup.getIsDefault());
        vo.setIcon(defaultGroup.getIcon());
        vo.setColor(defaultGroup.getColor());
        vo.setSortOrder(defaultGroup.getSortOrder());
        vo.setCreateTime(defaultGroup.getCreateTime());

        return R.ok(vo);
    }
}
