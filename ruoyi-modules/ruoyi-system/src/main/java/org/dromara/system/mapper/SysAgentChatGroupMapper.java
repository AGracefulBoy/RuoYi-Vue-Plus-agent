package org.dromara.system.mapper;

import org.apache.ibatis.annotations.Param;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysAgentChatGroup;
import org.dromara.system.domain.vo.ChatGroupVo;

import java.util.List;

/**
 * 智能体会话分组 数据层
 *
 * @author zhoudashuai
 */
public interface SysAgentChatGroupMapper extends BaseMapperPlus<SysAgentChatGroup, ChatGroupVo> {

    /**
     * 获取用户的分组列表（包含会话数量）
     *
     * @param userId 用户ID
     * @return 分组列表
     */
    List<ChatGroupVo> selectGroupsWithChatCount(@Param("userId") Long userId);

    /**
     * 获取用户的默认分组
     *
     * @param userId 用户ID
     * @return 默认分组
     */
    SysAgentChatGroup selectDefaultGroup(@Param("userId") Long userId);

    /**
     * 批量删除分组
     *
     * @param groupIds 分组ID列表
     * @return 删除行数
     */
    int deleteByIds(@Param("ids") List<Long> groupIds);
}