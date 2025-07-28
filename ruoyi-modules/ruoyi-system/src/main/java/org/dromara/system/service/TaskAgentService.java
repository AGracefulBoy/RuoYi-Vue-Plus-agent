package org.dromara.system.service;

import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.dto.TaskAgentDto.*;
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
     * @return 流式响应
     */
    Flux<String> executeReActStream(SysAgent agent,List<ToolDto> availableTools, String userInput);

    /**
     * 流式ReAct处理（带完整响应）
     *
     * @param agent 智能体信息
     * @param availableTools 可用工具列表
     * @param userInput 用户输入
     * @return 包含流式响应和完整响应的结果对象
     */
    TaskAgentServiceImpl.StreamResult executeReActStreamWithFullResponse(SysAgent agent, List<ToolDto> availableTools, String userInput);

    /**
     * 检查退出条件
     *
     * @param input 用户输入
     * @return 是否退出
     */
    boolean checkExitCondition(String input);
}
