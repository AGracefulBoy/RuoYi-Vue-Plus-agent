package org.dromara.system.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.vo.SysAgentChatDetailVo;
import reactor.core.publisher.Flux;

public interface SysAgentChatService {

    /**
     * 处理流式对话完成
     *
     * @param chatRequest 聊天请求参数
     * @return 流式响应
     */
    Flux<StreamMessageResponseDto> completions(ChatRequestDto chatRequest);

    /**
     * 根据智能体ID分页查询会话记录
     *
     * @param agentId 智能体ID
     * @param chatModel 对话模式
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    TableDataInfo<SysAgentChatDetailVo> queryPageListByAgent(Long agentId, String chatModel, PageQuery pageQuery);

    /**
     * 删除指定智能体的debug模式对话记录
     *
     * @param agentId 智能体ID
     * @return 删除结果
     */
    boolean deleteDebugChatsByAgentId(Long agentId);

    /**
     * 取消正在进行的操作
     *
     * @param traceId 追踪ID
     */
    void cancelOngoingOperations(String traceId);


    /**
     * 清理资源
     *
     * @param traceId 追踪ID
     */
    void cleanupResources(String traceId);

}
