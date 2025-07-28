package org.dromara.system.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.ChatResponseDto;
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
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public Flux<String> completions(ChatRequestDto chatRequest) {
        try {
            log.info("开始处理智能体对话，智能体ID: {}, 用户消息: {}",
                chatRequest.getAgentId(), chatRequest.getMessage());

            // 1. 获取智能体信息
            SysAgent agent = getAgentById(chatRequest.getAgentId());

            // 2. 构建可用工具列表
            List<ToolDto> availableTools = buildAvailableToolsList(agent);

            // 3. 根据智能体类型选择处理方式
            if ("task".equals(agent.getAgentType())) {
                // 任务类型智能体：使用ReAct思维链处理
                return handleTaskAgent(agent, availableTools, chatRequest);
            } else {
                // 其他类型智能体：使用原有逻辑
                return generateStreamResponse(agent, availableTools, chatRequest);
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
    private Flux<String> handleTaskAgent(SysAgent agent, List<ToolDto> availableTools, ChatRequestDto chatRequest) {
        // 创建或获取任务记忆
        String chatId = StringUtils.hasText(chatRequest.getChatId()) ?
            chatRequest.getChatId() :
            "chat_" + agent.getAgentId() + "_" + System.currentTimeMillis();


        // 检查退出条件
        if (taskAgentService.checkExitCondition(chatRequest.getMessage())) {
            return Flux.just("检测到退出指令，对话结束！感谢您的使用。");
        }

        // 执行ReAct思维链处理
        return taskAgentService.executeReActStream(agent, availableTools, chatRequest.getMessage())
            .doOnComplete(() -> {
                // 保存记忆
                log.info("任务智能体对话完成，会话ID: {}", chatId);
            })
            .doOnError(error -> {
                log.error("任务智能体处理失败，会话ID: {}", chatId, error);
            });
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
            .name(datasource.getDatasourceName())
            .desc(StringUtils.hasText(datasource.getDescription()) ?
                datasource.getDescription() : "数据源: " + datasource.getDatasourceName())
            .parameters(parameters)
            .build();
    }

    /**
     * 生成流式响应
     *
     * @param agent          智能体信息
     * @param availableTools 可用工具列表
     * @param chatRequest    聊天请求
     * @return 流式响应
     */
    private Flux<String> generateStreamResponse(SysAgent agent, List<ToolDto> availableTools, ChatRequestDto chatRequest) {
        // 模拟流式响应（实际项目中这里应该调用AI模型API）
        return Flux.interval(Duration.ofMillis(100))
            .take(10)
            .map(i -> buildResponseMessage(i, agent, availableTools, chatRequest.getMessage()))
            .doOnComplete(() -> log.info("流式对话完成"));
    }

    /**
     * 构建响应消息
     *
     * @param step           当前步骤
     * @param agent          智能体信息
     * @param availableTools 可用工具列表
     * @param userMessage    用户输入的消息
     * @return 响应消息
     */
    private String buildResponseMessage(Long step, SysAgent agent, List<ToolDto> availableTools, String userMessage) {
        return switch (step.intValue()) {
            case 0 -> "智能体 [" + agent.getAgentName() + "] 正在处理您的问题...\n";
            case 1 -> "可用工具: " + availableTools.size() + " 个\n";
            case 2 -> "智能体提示词: " + getProcessedPromptPreview(agent, availableTools, userMessage) + "\n";
            case 9 -> "处理完成。\n";
            default -> "正在思考中... (" + step + "/10)\n";
        };
    }

    /**
     * 获取提示词预览
     *
     * @param promptContent 提示词内容
     * @return 提示词预览
     */
    private String getPromptPreview(String promptContent) {
        if (!StringUtils.hasText(promptContent)) {
            return "未设置";
        }
        int maxLength = 50;
        return promptContent.length() > maxLength ?
            promptContent.substring(0, maxLength) + "..." :
            promptContent;
    }

    /**
     * 获取处理后的提示词预览（替换变量后）
     *
     * @param agent          智能体信息
     * @param availableTools 可用工具列表
     * @param userMessage    用户输入的消息
     * @return 处理后的提示词预览
     */
    private String getProcessedPromptPreview(SysAgent agent, List<ToolDto> availableTools, String userMessage) {
        String processedPrompt = processPromptTemplate(agent, availableTools, userMessage);
        return getPromptPreview(processedPrompt);
    }

    /**
     * 处理提示词模板，替换其中的变量
     *
     * @param agent          智能体信息
     * @param availableTools 可用工具列表
     * @param userMessage    用户输入的消息
     * @return 处理后的提示词
     */
    private String processPromptTemplate(SysAgent agent, List<ToolDto> availableTools, String userMessage) {
        String promptContent = agent.getPromptContent();

        if (!StringUtils.hasText(promptContent)) {
            return "未设置提示词";
        }

        // 替换 {{agent_personality}} 为智能体人设
        String agentPersonality = StringUtils.hasText(agent.getAgentPersonality()) ?
            agent.getAgentPersonality() : "通用智能助手";
        promptContent = promptContent.replace("{{agent_personality}}", agentPersonality);

        // 替换 {{tool_list}} 为可用工具列表
        String toolList = buildToolListDescription(availableTools);
        promptContent = promptContent.replace("{{tool_list}}", toolList);

        // 替换 {{query}} 为用户输入的消息
        String query = StringUtils.hasText(userMessage) ? userMessage : "";
        promptContent = promptContent.replace("{{query}}", query);

        log.debug("提示词模板处理完成，原长度: {}, 处理后长度: {}",
            agent.getPromptContent().length(), promptContent.length());

        return promptContent;
    }

    /**
     * 构建工具列表描述
     *
     * @param availableTools 可用工具列表
     * @return 工具列表描述字符串
     */
    private String buildToolListDescription(List<ToolDto> availableTools) {
        if (CollectionUtils.isEmpty(availableTools)) {
            return "无可用工具";
        }

        StringBuilder toolDescription = new StringBuilder();
        toolDescription.append("可用工具列表:\n");

        for (int i = 0; i < availableTools.size(); i++) {
            ToolDto tool = availableTools.get(i);
            toolDescription.append(String.format("%d. %s: %s", i + 1, tool.getName(), tool.getDesc()));

            // 添加参数信息
            if (!CollectionUtils.isEmpty(tool.getParameters())) {
                toolDescription.append("\n   参数: ");
                List<String> paramNames = tool.getParameters().stream()
                    .map(param -> param.getName() + "(" + param.getType() + ")" +
                        (param.getRequired() ? "*" : ""))
                    .collect(Collectors.toList());
                toolDescription.append(String.join(", ", paramNames));
            }

            if (i < availableTools.size() - 1) {
                toolDescription.append("\n");
            }
        }

        return toolDescription.toString();
    }

    /**
     * 构建工具参数
     */
    private List<ToolDto.Parameter> buildToolParameters(SysTool tool) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();

        // 基础参数
        parameters.add(ToolDto.Parameter.builder()
            .name("toolType")
            .desc("工具类型")
            .type("string")
            .required(true)
            .build());

        parameters.add(ToolDto.Parameter.builder()
            .name("functionName")
            .desc("函数名称")
            .type("string")
            .required(true)
            .build());

        if ("1".equals(tool.getIsStream())) {
            parameters.add(ToolDto.Parameter.builder()
                .name("stream")
                .desc("是否支持流式处理")
                .type("boolean")
                .required(false)
                .build());
        }

        return parameters;
    }

    /**
     * 构建知识库参数
     */
    private List<ToolDto.Parameter> buildKnowledgeBaseParameters(SysKnowledgeBase kb) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();

        parameters.add(ToolDto.Parameter.builder()
            .name("query")
            .desc("查询内容")
            .type("string")
            .required(true)
            .build());

        if (kb.getTopK() != null) {
            parameters.add(ToolDto.Parameter.builder()
                .name("topK")
                .desc("检索返回条数")
                .type("integer")
                .required(false)
                .build());
        }

        if (kb.getVectorWeight() != null) {
            parameters.add(ToolDto.Parameter.builder()
                .name("vectorWeight")
                .desc("向量检索权重")
                .type("number")
                .required(false)
                .build());
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

    @Override
    public Mono<ChatResponseDto> completionsSync(ChatRequestDto chatRequest) {
        return null;
    }
}


