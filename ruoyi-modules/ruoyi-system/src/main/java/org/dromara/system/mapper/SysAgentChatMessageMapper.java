package org.dromara.system.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.system.domain.SysAgentChatMessage;

import java.util.List;

/**
 * 智能体会话消息 Mapper接口
 *
 * @author zhoudashuai
 */
public interface SysAgentChatMessageMapper extends BaseMapperPlus<SysAgentChatMessage, SysAgentChatMessage> {

    /**
     * 批量插入消息
     *
     * @param messages 消息列表
     * @return 插入数量
     */
    int insertBatch(List<SysAgentChatMessage> messages);
}