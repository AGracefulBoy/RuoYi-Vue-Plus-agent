package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.mapper.SysAgentMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.service.SysAgentChatService;
import org.dromara.system.service.TaskAgentService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAgentChatServiceImpl implements SysAgentChatService {

    private final SysAgentMapper agentMapper;
    private final SysToolMapper toolMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;
    private final SysDatasourceMapper datasourceMapper;
    private final TaskAgentService taskAgentService;

    @Override
    public Flux<StreamMessageResponseDto> completions(ChatRequestDto chatRequest) {
        try {
            log.info("开始处理智能体对话，智能体ID: {}, 用户消息: {}",
                chatRequest.getAgentId(), chatRequest.getMessage());

            // 1. 获取智能体信息
            SysAgent agent = getAgentById(chatRequest.getAgentId());

            // 2. 构建可用工具列表
            List<ToolDto> availableTools = buildAvailableToolsList(agent);

            // 3. 根据对话模式选择处理方式
            if ("self_planning".equals(agent.getConversationMode())) {
                // 自主规划模式：使用ReAct思维链处理
                log.info("智能体{}使用自主规划模式处理对话", agent.getAgentId());
                return handleTaskAgent(agent, availableTools, chatRequest);
            } else {
                // 自由对话模式：直接对话，不使用思维链
                log.info("智能体{}使用自由对话模式处理对话", agent.getAgentId());
                return handleFreeChatAgent(agent, chatRequest);
            }

        } catch (Exception e) {
            log.error("处理智能体对话时发生错误", e);
            return Flux.error(e);
        }
    }

    /**
     * 处理任务类型智能体（使用ReAct思维链）
     *
     * @param agent          智能体信息
     * @param availableTools 可用工具列表
     * @param chatRequest    聊天请求
     * @return 流式响应
     */
    private Flux<StreamMessageResponseDto> handleTaskAgent(SysAgent agent, List<ToolDto> availableTools, ChatRequestDto chatRequest) {
        // 创建或获取任务记忆
        String chatId = StringUtils.hasText(chatRequest.getChatId()) ?
            chatRequest.getChatId() :
            "chat_" + agent.getAgentId() + "_" + System.currentTimeMillis();

        // 检查退出条件
        if (taskAgentService.checkExitCondition(chatRequest.getMessage())) {
            StreamMessageResponseDto exitResponse = StreamMessageResponseDto.createAnswerMessage(
                "检测到退出指令，对话结束！感谢您的使用。",
                chatId,
                "exit_" + System.currentTimeMillis(),
                null,
                0,
                true
            );
            return Flux.just(exitResponse);
        }

        // 执行ReAct思维链 - 直接返回，不添加额外的操作符以避免上下文丢失
        // 日志记录已经在 TaskAgentServiceImpl 内部处理
        return taskAgentService.executeReActStream(agent, availableTools, chatRequest.getMessage());
    }

    /**
     * 处理自由对话模式智能体
     *
     * @param agent       智能体信息
     * @param chatRequest 聊天请求
     * @return 流式响应
     */
    private Flux<StreamMessageResponseDto> handleFreeChatAgent(SysAgent agent, ChatRequestDto chatRequest) {
        // 创建或获取会话ID
        String chatId = StringUtils.hasText(chatRequest.getChatId()) ?
            chatRequest.getChatId() :
            "chat_" + agent.getAgentId() + "_" + System.currentTimeMillis();

        // 检查退出条件
        if (taskAgentService.checkExitCondition(chatRequest.getMessage())) {
            StreamMessageResponseDto exitResponse = StreamMessageResponseDto.createAnswerMessage(
                "检测到退出指令，对话结束！感谢您的使用。",
                chatId,
                "exit_" + System.currentTimeMillis(),
                null,
                0,
                true
            );
            return Flux.just(exitResponse);
        }

        // 执行自由对话处理 - 直接返回，不添加额外的操作符以避免上下文丢失
        // 日志记录已经在 TaskAgentServiceImpl 内部处理
        return taskAgentService.executeFreeChatStream(agent, chatRequest.getMessage());
    }

    /**
     * 根据ID获取智能体信息
     *
     * @param agentId 智能体ID
     * @return 智能体信息
     */
    private SysAgent getAgentById(Long agentId) {
        SysAgent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            throw new RuntimeException("智能体不存在，ID: " + agentId);
        }
        log.debug("成功获取智能体信息: {}", agent.getAgentName());
        return agent;
    }

    /**
     * 构建智能体可用工具列表
     *
     * @param agent 智能体信息
     * @return 可用工具列表
     */
    private List<ToolDto> buildAvailableToolsList(SysAgent agent) {
        List<ToolDto> toolDtoList = new ArrayList<>();

        // 添加工具信息
        toolDtoList.addAll(getToolsFromAgent(agent));

        // 添加知识库信息
        toolDtoList.addAll(getKnowledgeBasesFromAgent(agent));

        // 添加数据源信息
        toolDtoList.addAll(getDatasourcesFromAgent(agent));

        log.info("智能体 {} 总计可用资源: {} 个", agent.getAgentName(), toolDtoList.size());
        return toolDtoList;
    }

    /**
     * 从智能体配置中获取工具信息
     *
     * @param agent 智能体信息
     * @return 工具DTO列表
     */
    private List<ToolDto> getToolsFromAgent(SysAgent agent) {
        List<ToolDto> toolDtos = new ArrayList<>();

        if (CollectionUtils.isEmpty(agent.getToolList())) {
            return toolDtos;
        }

        List<SysTool> tools = toolMapper.selectBatchIds(agent.getToolList());
        for (SysTool tool : tools) {
            ToolDto toolDto = convertToolToDto(tool);
            toolDtos.add(toolDto);
        }

        log.debug("查询到工具信息，数量: {}", tools.size());
        return toolDtos;
    }

    /**
     * 从智能体配置中获取知识库信息
     *
     * @param agent 智能体信息
     * @return 知识库DTO列表
     */
    private List<ToolDto> getKnowledgeBasesFromAgent(SysAgent agent) {
        List<ToolDto> knowledgeBaseDtos = new ArrayList<>();

        if (CollectionUtils.isEmpty(agent.getKnowledgeBaseList())) {
            return knowledgeBaseDtos;
        }

        List<SysKnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectBatchIds(agent.getKnowledgeBaseList());
        for (SysKnowledgeBase kb : knowledgeBases) {
            ToolDto toolDto = convertKnowledgeBaseToDto(kb);
            knowledgeBaseDtos.add(toolDto);
        }

        log.debug("查询到知识库信息，数量: {}", knowledgeBases.size());
        return knowledgeBaseDtos;
    }

    /**
     * 从智能体配置中获取数据源信息
     *
     * @param agent 智能体信息
     * @return 数据源DTO列表
     */
    private List<ToolDto> getDatasourcesFromAgent(SysAgent agent) {
        List<ToolDto> datasourceDtos = new ArrayList<>();

        if (CollectionUtils.isEmpty(agent.getDatabaseList())) {
            return datasourceDtos;
        }

        List<SysDatasource> datasources = datasourceMapper.selectBatchIds(agent.getDatabaseList());
        for (SysDatasource datasource : datasources) {
            ToolDto toolDto = convertDatasourceToDto(datasource);
            datasourceDtos.add(toolDto);
        }

        log.debug("查询到数据源信息，数量: {}", datasources.size());
        return datasourceDtos;
    }

    /**
     * 将工具实体转换为ToolDto
     *
     * @param tool 工具实体
     * @return ToolDto
     */
    private ToolDto convertToolToDto(SysTool tool) {
        List<ToolDto.Parameter> parameters = buildToolParameters(tool);
        return ToolDto.builder()
            .id(tool.getToolId())
            .type("tool")
            .name(tool.getToolName())
            .desc(StringUtils.hasText(tool.getToolDesc()) ? tool.getToolDesc() : "工具: " + tool.getToolName())
            .parameters(parameters)
            .build();
    }

    /**
     * 将知识库实体转换为ToolDto
     *
     * @param knowledgeBase 知识库实体
     * @return ToolDto
     */
    private ToolDto convertKnowledgeBaseToDto(SysKnowledgeBase knowledgeBase) {
        List<ToolDto.Parameter> parameters = buildKnowledgeBaseParameters(knowledgeBase);
        return ToolDto.builder()
            .id(knowledgeBase.getKnowledgeBaseId())
            .type("knowledge")
            .name(knowledgeBase.getName())
            .desc(StringUtils.hasText(knowledgeBase.getDescription()) ?
                knowledgeBase.getDescription() : "知识库: " + knowledgeBase.getName())
            .parameters(parameters)
            .build();
    }

    /**
     * 将数据源实体转换为ToolDto
     *
     * @param datasource 数据源实体
     * @return ToolDto
     */
    private ToolDto convertDatasourceToDto(SysDatasource datasource) {
        List<ToolDto.Parameter> parameters = buildDatasourceParameters(datasource);
        return ToolDto.builder()
            .id(datasource.getDatasourceId())
            .type("datasource")
            .name(datasource.getDatasourceName())
            .desc(StringUtils.hasText(datasource.getDescription()) ?
                datasource.getDescription() : "数据源: " + datasource.getDatasourceName())
            .parameters(parameters)
            .build();
    }


    /**
     * 构建工具参数
     */
    private List<ToolDto.Parameter> buildToolParameters(SysTool tool) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();

        return parameters;
    }

    /**
     * 构建知识库参数
     */
    private List<ToolDto.Parameter> buildKnowledgeBaseParameters(SysKnowledgeBase kb) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();

        parameters.add(ToolDto.Parameter.builder()
            .name("question")
            .desc("查询内容")
            .type("string")
            .required(true)
            .build());

        if (kb.getMetadata() != null && !kb.getMetadata().isEmpty()) {
            try {
                // Parse metadata JSON string to Map
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = JSONUtil.toBean(kb.getMetadata(), Map.class);

                // Create parameters for each metadata key
                if (metadataMap != null) {
                    for (Map.Entry<String, Object> entry : metadataMap.entrySet()) {
                        parameters.add(ToolDto.Parameter.builder()
                            .name(entry.getKey())
                            .desc(entry.getValue() != null ? entry.getValue().toString() : entry.getKey())
                            .type("string")
                            .required(true)
                            .build());
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse knowledge base metadata: {}", kb.getMetadata(), e);
            }
        }

        return parameters;
    }

    /**
     * 构建数据源参数
     */
    private List<ToolDto.Parameter> buildDatasourceParameters(SysDatasource datasource) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();

        parameters.add(ToolDto.Parameter.builder()
            .name("datasourceType")
            .desc("数据源类型")
            .type("string")
            .required(true)
            .build());

        parameters.add(ToolDto.Parameter.builder()
            .name("databaseType")
            .desc("数据库类型")
            .type("string")
            .required(false)
            .build());

        if ("database".equals(datasource.getDatasourceType())) {
            parameters.add(ToolDto.Parameter.builder()
                .name("sql")
                .desc("SQL查询语句")
                .type("string")
                .required(true)
                .build());
        }

        if (datasource.getQueryTimeout() != null) {
            parameters.add(ToolDto.Parameter.builder()
                .name("timeout")
                .desc("查询超时时间(毫秒)")
                .type("integer")
                .required(false)
                .build());
        }

        return parameters;
    }
}


