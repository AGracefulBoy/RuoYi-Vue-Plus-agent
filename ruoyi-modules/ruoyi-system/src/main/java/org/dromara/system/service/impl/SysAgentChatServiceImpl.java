package org.dromara.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.system.domain.*;
import org.dromara.system.domain.dto.ChatRequestDto;
import org.dromara.system.domain.dto.StreamMessageResponseDto;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.domain.vo.SysAgentChatDetailVo;
import org.dromara.system.mapper.*;
import org.dromara.system.service.AgentLocalCacheService;
import org.dromara.system.service.ChatContextService;
import org.dromara.system.service.SysAgentChatService;
import org.dromara.system.service.TaskAgentService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAgentChatServiceImpl implements SysAgentChatService {

    private final SysAgentMapper agentMapper;
    private final SysAgentChatMapper agentChatMapper;
    private final SysAgentChatMessageMapper agentChatMessageMapper;
    private final SysToolMapper toolMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;
    private final SysDatasourceMapper datasourceMapper;
    private final TaskAgentService taskAgentService;
    private final ChatContextService chatContextService;
    private final AgentLocalCacheService agentLocalCacheService;

    @Override
    public Flux<StreamMessageResponseDto> completions(ChatRequestDto chatRequest) {
        try {
            long startTime = System.currentTimeMillis();
            log.info("开始处理智能体对话 - 智能体ID: {}, 用户消息: {}",
                chatRequest.getAgentId(), chatRequest.getMessage());

            // 1. 获取智能体信息（直接从数据库查询）
            SysAgent agent = agentMapper.selectById(chatRequest.getAgentId());
            if (agent == null) {
                throw new RuntimeException("智能体不存在，ID: " + chatRequest.getAgentId());
            }
            log.debug("智能体信息获取完成 - 名称: {}, 耗时: {}ms",
                agent.getAgentName(), System.currentTimeMillis() - startTime);

            // 2. 构建可用工具列表（直接从数据库查询）
            long toolStartTime = System.currentTimeMillis();
            List<ToolDto> availableTools = buildAvailableToolsDirectly(agent);
            log.info("可用工具加载完成 - 数量: {}, 耗时: {}ms",
                availableTools.size(), System.currentTimeMillis() - toolStartTime);

            // 3. 根据对话模式选择处理方式
            if ("self_planning".equals(agent.getConversationMode())) {
                // 自主规划模式：使用ReAct思维链处理
                log.info("智能体{}使用自主规划模式处理对话 - 准备耗时: {}ms",
                    agent.getAgentId(), System.currentTimeMillis() - startTime);
                return handleTaskAgent(agent, availableTools, chatRequest);
            } else {
                // 自由对话模式：直接对话，不使用思维链
                log.info("智能体{}使用自由对话模式处理对话 - 准备耗时: {}ms",
                    agent.getAgentId(), System.currentTimeMillis() - startTime);
                return handleFreeChatAgent(agent, chatRequest);
            }

        } catch (Exception e) {
            log.error("处理智能体对话时发生错误 - 智能体ID: {}, 错误信息: {}",
                chatRequest.getAgentId(), e.getMessage(), e);
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
        // 获取当前用户ID
        Long userId = LoginHelper.getUserId();

        // 创建或获取会话
        SysAgentChat chat;
        String chatModel = chatRequest.getChatModel();
        Long groupId = chatRequest.getGroupId();

        chat = chatContextService.createChat(agent.getAgentId(), userId,
            chatRequest.getMessage(), groupId, chatModel);

        // 检查退出条件
        if (taskAgentService.checkExitCondition(chatRequest.getMessage())) {
            StreamMessageResponseDto exitResponse = StreamMessageResponseDto.createAnswerMessage(
                "检测到退出指令，对话结束！感谢您的使用。",
                String.valueOf(chat.getChatId()),
                "exit_" + System.currentTimeMillis(),
                null,
                0,
                true
            );
            return Flux.just(exitResponse);
        }

        // 执行ReAct思维链 - 传递会话ID
        return taskAgentService.executeReActStream(agent, availableTools, chatRequest.getMessage(), chat.getChatId());
    }

    /**
     * 处理自由对话模式智能体
     *
     * @param agent       智能体信息
     * @param chatRequest 聊天请求
     * @return 流式响应
     */
    private Flux<StreamMessageResponseDto> handleFreeChatAgent(SysAgent agent, ChatRequestDto chatRequest) {
        // 获取当前用户ID
        Long userId = LoginHelper.getUserId();



        String chatModel = chatRequest.getChatModel();
        Long groupId = chatRequest.getGroupId();
        // 创建或获取会话
        SysAgentChat chat = chatContextService.createChat(agent.getAgentId(), userId,
            chatRequest.getMessage(), groupId, chatModel);
        // 检查退出条件
        if (taskAgentService.checkExitCondition(chatRequest.getMessage())) {
            StreamMessageResponseDto exitResponse = StreamMessageResponseDto.createAnswerMessage(
                "检测到退出指令，对话结束！感谢您的使用。",
                String.valueOf(chat.getChatId()),
                "exit_" + System.currentTimeMillis(),
                null,
                0,
                true
            );
            return Flux.just(exitResponse);
        }

        // 执行自由对话处理 - 传递会话ID
        return taskAgentService.executeFreeChatStream(agent, chatRequest.getMessage(), chat.getChatId());
    }

    /**
     * 根据ID获取智能体信息
     *
     * @deprecated 使用 agentLocalCacheService.getAgent() 代替
     */
    @Deprecated
    private SysAgent getAgentById(Long agentId) {
        return agentLocalCacheService.getAgent(agentId);
    }

    /**
     * 构建智能体可用工具列表
     *
     * @deprecated 使用 agentLocalCacheService.getAvailableTools() 代替
     */
    @Deprecated
    private List<ToolDto> buildAvailableToolsList(SysAgent agent) {
        return agentLocalCacheService.getAvailableTools(agent.getAgentId());
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
            .desc(StringUtils.isNotBlank(tool.getToolDesc()) ? tool.getToolDesc() : "工具: " + tool.getToolName())
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
            .desc(StringUtils.isNotBlank(knowledgeBase.getDescription()) ?
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
            .desc(StringUtils.isNotBlank(datasource.getDescription()) ?
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
            // 按逗号分割元数据字符串
            String[] items = kb.getMetadata().split(",");
            for (String item : items) {
                String trimmedItem = item.trim();
                if (StringUtils.isNotBlank(trimmedItem)) {
                    String paramName;
                    String paramDesc;

                    // 如果包含等号，则分割为键值对
                    if (trimmedItem.contains("=")) {
                        String[] keyValue = trimmedItem.split("=", 2);
                        if (keyValue.length == 2) {
                            paramName = keyValue[0].trim();
                            paramDesc = keyValue[1].trim();
                        } else {
                            paramName = trimmedItem;
                            paramDesc = trimmedItem;
                        }
                    } else {
                        // 否则将项目作为键和描述
                        paramName = trimmedItem;
                        paramDesc = trimmedItem;
                    }

                    parameters.add(ToolDto.Parameter.builder()
                        .name(paramName)
                        .desc(paramDesc)
                        .type("string")
                        .required(true)
                        .build());
                }
            }
        }

        return parameters;
    }

    @Override
    public TableDataInfo<SysAgentChatDetailVo> queryPageListByAgent(Long agentId, String chatModel, PageQuery pageQuery) {
        // 构建查询条件
        LambdaQueryWrapper<SysAgentChat> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAgentChat::getAgentId, agentId)
            .eq(StringUtils.isNotBlank(chatModel),
                SysAgentChat::getChatModel, chatModel)
            .eq(SysAgentChat::getDelFlag, "0")
            .orderByDesc(SysAgentChat::getCreateTime);

        // 执行分页查询
        Page<SysAgentChatDetailVo> result = agentChatMapper.selectVoPage(
            pageQuery.build(), wrapper, SysAgentChatDetailVo.class);

        // 为每个会话查询对应的消息列表
        if (result.getRecords() != null && !result.getRecords().isEmpty()) {
            for (SysAgentChatDetailVo vo : result.getRecords()) {
                // 查询该会话的所有消息
                LambdaQueryWrapper<SysAgentChatMessage> messageWrapper = Wrappers.lambdaQuery();
                messageWrapper.eq(SysAgentChatMessage::getChatId, vo.getChatId())
                    .eq(SysAgentChatMessage::getDelFlag, "0")
                    .orderByAsc(SysAgentChatMessage::getMessageIndex);

                List<SysAgentChatMessage> messages = agentChatMessageMapper.selectList(messageWrapper);
                vo.setMessages(messages);
            }
        }

        return TableDataInfo.build(result);
    }

    @Override
    public boolean deleteDebugChatsByAgentId(Long agentId) {
        // 构建查询条件
        LambdaQueryWrapper<SysAgentChat> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysAgentChat::getAgentId, agentId)
            .eq(SysAgentChat::getChatModel, "debug")
            .eq(SysAgentChat::getDelFlag, "0");

        // 逻辑删除（MyBatis-Plus会自动将del_flag设置为'1'）
        return agentChatMapper.delete(wrapper) > 0;
    }

    /**
     * 直接从数据库构建可用工具列表
     * 整合工具、知识库和数据源的查询
     */
    private List<ToolDto> buildAvailableToolsDirectly(SysAgent agent) {
        List<ToolDto> allTools = new ArrayList<>();

        // 1. 获取工具列表
        List<ToolDto> tools = getToolsFromAgent(agent);
        allTools.addAll(tools);

        // 2. 获取知识库列表
        List<ToolDto> knowledgeBases = getKnowledgeBasesFromAgent(agent);
        allTools.addAll(knowledgeBases);

        // 3. 获取数据源列表
        List<ToolDto> datasources = getDatasourcesFromAgent(agent);
        allTools.addAll(datasources);

        log.debug("构建工具列表完成 - 总数: {}, 工具: {}, 知识库: {}, 数据源: {}",
            allTools.size(), tools.size(), knowledgeBases.size(), datasources.size());

        return allTools;
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
    public void cancelOngoingOperations(String traceId) {
        log.info("取消正在进行的操作 - 追踪ID: {}", traceId);
        // 通知TaskAgentService取消操作
        taskAgentService.cancelOperation(traceId);
    }

    @Override
    public void updateChatStatus(Long chatId, String status) {
        if (chatId == null) {
            return;
        }
        
        SysAgentChat chat = agentChatMapper.selectById(chatId);
        if (chat != null) {
            chat.setStatus(status);
            chat.setEndTime(new Date());
            agentChatMapper.updateById(chat);
            log.info("更新会话状态 - 会话ID: {}, 状态: {}", chatId, status);
        }
    }

    @Override
    public void cleanupResources(String traceId) {
        log.debug("清理资源 - 追踪ID: {}", traceId);
        // 清理可能的缓存
        if (agentLocalCacheService != null) {
            // 清理缓存中的临时数据
        }
    }
}


