package org.dromara.system.service;

import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.service.impl.TaskAgentServiceImpl;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 任务智能体服务接口
 *
 * @author assistant
 */
public interface TaskAgentService {
  /**
     * 流式ReAct处理
     *
     * @param agent 智能体信息
     * @param availableTools 可用工具列表
     * @param userInput 用户输入
     * @param chatId 会话ID
     * @return 流式响应
     */
    Flux<StreamMessageResponseDto> executeReActStream(SysAgent agent,List<ToolDto> availableTools, String userInput, Long chatId);

    /**
     * 检查退出条件
     *
     * @param input 用户输入
     * @return 是否退出
     */
    boolean checkExitCondition(String input);

    /**
     * 执行自由对话模式的流式处理
     *
     * @param agent 智能体信息
     * @param userInput 用户输入
     * @param chatId 会话ID
     * @return 流式响应
     */
    Flux<StreamMessageResponseDto> executeFreeChatStream(SysAgent agent, String userInput, Long chatId);

    /**
     * 取消正在进行的操作
     *
     * @param traceId 追踪ID
     */
    void cancelOperation(String traceId);
}
