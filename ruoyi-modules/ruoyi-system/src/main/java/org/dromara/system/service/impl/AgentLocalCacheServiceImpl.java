package org.dromara.system.service.impl;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.SysDatasource;
import org.dromara.system.domain.SysKnowledgeBase;
import org.dromara.system.domain.SysTool;
import org.dromara.system.domain.dto.ToolDto;
import org.dromara.system.mapper.SysAgentMapper;
import org.dromara.system.mapper.SysDatasourceMapper;
import org.dromara.system.mapper.SysKnowledgeBaseMapper;
import org.dromara.system.mapper.SysToolMapper;
import org.dromara.system.service.AgentLocalCacheService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 智能体本地缓存服务实现
 * 使用Caffeine实现高性能本地缓存，支持自动过期和手动失效
 *
 * @author system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLocalCacheServiceImpl implements AgentLocalCacheService {
    
    private final SysAgentMapper agentMapper;
    private final SysToolMapper toolMapper;
    private final SysKnowledgeBaseMapper knowledgeBaseMapper;
    private final SysDatasourceMapper datasourceMapper;
    
    // 并行查询线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    // 智能体信息缓存
    private Cache<Long, SysAgent> agentCache;
    
    // 工具列表缓存
    private Cache<Long, List<ToolDto>> toolsCache;
    
    @PostConstruct
    public void init() {
        // 初始化智能体缓存
        // 30秒后过期，最大500个条目，启用统计
        agentCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(500)
            .recordStats()
            .build();
        
        // 初始化工具缓存
        // 30秒后过期，最大500个条目，启用统计
        toolsCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .expireAfterAccess(10, TimeUnit.SECONDS)
            .maximumSize(500)
            .recordStats()
            .build();
        
        log.info("智能体本地缓存服务初始化完成");
    }
    
    @Override
    public SysAgent getAgent(Long agentId) {
        // 使用get方法，如果缓存不存在则自动加载
        return agentCache.get(agentId, this::loadAgent);
    }
    
    @Override
    public List<ToolDto> getAvailableTools(Long agentId) {
        // 使用get方法，如果缓存不存在则自动加载
        return toolsCache.get(agentId, this::loadAvailableTools);
    }
    
    @Override
    public void cacheAgent(SysAgent agent) {
        if (agent != null && agent.getAgentId() != null) {
            agentCache.put(agent.getAgentId(), agent);
            log.debug("缓存智能体信息 - agentId: {}", agent.getAgentId());
        }
    }
    
    @Override
    public void cacheAvailableTools(Long agentId, List<ToolDto> tools) {
        if (agentId != null && tools != null) {
            toolsCache.put(agentId, tools);
            log.debug("缓存工具列表 - agentId: {}, 工具数量: {}", agentId, tools.size());
        }
    }
    
    @Override
    public void evictAgent(Long agentId) {
        agentCache.invalidate(agentId);
        toolsCache.invalidate(agentId);
        log.info("清除智能体缓存 - agentId: {}", agentId);
    }
    
    @Override
    public void evictAll() {
        agentCache.invalidateAll();
        toolsCache.invalidateAll();
        log.info("清除所有智能体缓存");
    }
    
    @Override
    public void evictAgents(List<Long> agentIds) {
        if (!CollectionUtils.isEmpty(agentIds)) {
            agentCache.invalidateAll(agentIds);
            toolsCache.invalidateAll(agentIds);
            log.info("批量清除智能体缓存 - 数量: {}", agentIds.size());
        }
    }
    
    @Override
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 智能体缓存统计
        CacheStats agentStats = agentCache.stats();
        Map<String, Object> agentStatsMap = new HashMap<>();
        agentStatsMap.put("size", agentCache.estimatedSize());
        agentStatsMap.put("hitCount", agentStats.hitCount());
        agentStatsMap.put("missCount", agentStats.missCount());
        agentStatsMap.put("hitRate", agentStats.hitRate());
        agentStatsMap.put("loadCount", agentStats.loadCount());
        agentStatsMap.put("evictionCount", agentStats.evictionCount());
        stats.put("agentCache", agentStatsMap);
        
        // 工具缓存统计
        CacheStats toolsStats = toolsCache.stats();
        Map<String, Object> toolsStatsMap = new HashMap<>();
        toolsStatsMap.put("size", toolsCache.estimatedSize());
        toolsStatsMap.put("hitCount", toolsStats.hitCount());
        toolsStatsMap.put("missCount", toolsStats.missCount());
        toolsStatsMap.put("hitRate", toolsStats.hitRate());
        toolsStatsMap.put("loadCount", toolsStats.loadCount());
        toolsStatsMap.put("evictionCount", toolsStats.evictionCount());
        stats.put("toolsCache", toolsStatsMap);
        
        return stats;
    }
    
    @Override
    public void warmUp(List<Long> agentIds) {
        if (CollectionUtils.isEmpty(agentIds)) {
            return;
        }
        
        log.info("开始预热缓存 - 智能体数量: {}", agentIds.size());
        
        // 异步预热
        CompletableFuture.runAsync(() -> {
            for (Long agentId : agentIds) {
                try {
                    // 加载智能体信息
                    SysAgent agent = loadAgent(agentId);
                    if (agent != null) {
                        agentCache.put(agentId, agent);
                        // 加载工具列表
                        List<ToolDto> tools = loadAvailableTools(agentId);
                        toolsCache.put(agentId, tools);
                    }
                } catch (Exception e) {
                    log.error("预热缓存失败 - agentId: {}", agentId, e);
                }
            }
            log.info("缓存预热完成");
        }, executorService);
    }
    
    @Override
    public void refresh(Long agentId) {
        log.info("刷新智能体缓存 - agentId: {}", agentId);
        
        // 先失效
        evictAgent(agentId);
        
        // 重新加载
        SysAgent agent = loadAgent(agentId);
        if (agent != null) {
            agentCache.put(agentId, agent);
            List<ToolDto> tools = loadAvailableTools(agentId);
            toolsCache.put(agentId, tools);
        }
    }
    
    /**
     * 从数据库加载智能体信息
     */
    private SysAgent loadAgent(Long agentId) {
        long startTime = System.currentTimeMillis();
        log.debug("从数据库加载智能体 - agentId: {}", agentId);
        
        SysAgent agent = agentMapper.selectById(agentId);
        if (agent == null) {
            log.warn("智能体不存在 - agentId: {}", agentId);
            throw new RuntimeException("智能体不存在，ID: " + agentId);
        }
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("智能体加载完成 - agentId: {}, 名称: {}, 耗时: {}ms", 
            agentId, agent.getAgentName(), duration);
        
        return agent;
    }
    
    /**
     * 从数据库加载可用工具列表
     */
    private List<ToolDto> loadAvailableTools(Long agentId) {
        long startTime = System.currentTimeMillis();
        log.debug("从数据库加载工具列表 - agentId: {}", agentId);
        
        // 先获取智能体信息
        SysAgent agent = getAgent(agentId);
        
        // 并行加载所有资源
        List<ToolDto> tools = buildAvailableToolsParallel(agent);
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("工具列表加载完成 - agentId: {}, 工具数量: {}, 耗时: {}ms", 
            agentId, tools.size(), duration);
        
        return tools;
    }
    
    /**
     * 并行构建可用工具列表
     */
    private List<ToolDto> buildAvailableToolsParallel(SysAgent agent) {
        List<ToolDto> result = new ArrayList<>();
        
        // 创建并行任务
        CompletableFuture<List<ToolDto>> toolsFuture = CompletableFuture.supplyAsync(() -> {
            if (CollectionUtils.isEmpty(agent.getToolList())) {
                return new ArrayList<>();
            }
            long start = System.currentTimeMillis();
            List<SysTool> tools = toolMapper.selectBatchIds(agent.getToolList());
            List<ToolDto> dtos = tools.stream()
                .map(this::convertToolToDto)
                .collect(Collectors.toList());
            log.debug("工具查询完成 - 数量: {}, 耗时: {}ms", 
                dtos.size(), System.currentTimeMillis() - start);
            return dtos;
        }, executorService);
        
        CompletableFuture<List<ToolDto>> kbFuture = CompletableFuture.supplyAsync(() -> {
            if (CollectionUtils.isEmpty(agent.getKnowledgeBaseList())) {
                return new ArrayList<>();
            }
            long start = System.currentTimeMillis();
            List<SysKnowledgeBase> kbs = knowledgeBaseMapper.selectBatchIds(agent.getKnowledgeBaseList());
            List<ToolDto> dtos = kbs.stream()
                .map(this::convertKnowledgeBaseToDto)
                .collect(Collectors.toList());
            log.debug("知识库查询完成 - 数量: {}, 耗时: {}ms", 
                dtos.size(), System.currentTimeMillis() - start);
            return dtos;
        }, executorService);
        
        CompletableFuture<List<ToolDto>> dsFuture = CompletableFuture.supplyAsync(() -> {
            if (CollectionUtils.isEmpty(agent.getDatabaseList())) {
                return new ArrayList<>();
            }
            long start = System.currentTimeMillis();
            List<SysDatasource> datasources = datasourceMapper.selectBatchIds(agent.getDatabaseList());
            List<ToolDto> dtos = datasources.stream()
                .map(this::convertDatasourceToDto)
                .collect(Collectors.toList());
            log.debug("数据源查询完成 - 数量: {}, 耗时: {}ms", 
                dtos.size(), System.currentTimeMillis() - start);
            return dtos;
        }, executorService);
        
        // 等待所有任务完成并合并结果
        CompletableFuture.allOf(toolsFuture, kbFuture, dsFuture).join();
        
        result.addAll(toolsFuture.join());
        result.addAll(kbFuture.join());
        result.addAll(dsFuture.join());
        
        return result;
    }
    
    /**
     * 将工具实体转换为ToolDto
     */
    private ToolDto convertToolToDto(SysTool tool) {
        return ToolDto.builder()
            .id(tool.getToolId())
            .type("tool")
            .name(tool.getToolName())
            .desc(StringUtils.hasText(tool.getToolDesc()) ? tool.getToolDesc() : "工具: " + tool.getToolName())
            .parameters(new ArrayList<>())
            .build();
    }
    
    /**
     * 将知识库实体转换为ToolDto
     */
    private ToolDto convertKnowledgeBaseToDto(SysKnowledgeBase knowledgeBase) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();
        
        parameters.add(ToolDto.Parameter.builder()
            .name("question")
            .desc("查询内容")
            .type("string")
            .required(true)
            .build());
        
        if (knowledgeBase.getMetadata() != null && !knowledgeBase.getMetadata().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> metadataMap = JSONUtil.toBean(knowledgeBase.getMetadata(), Map.class);
                
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
                log.warn("解析知识库元数据失败: {}", knowledgeBase.getMetadata(), e);
            }
        }
        
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
     */
    private ToolDto convertDatasourceToDto(SysDatasource datasource) {
        List<ToolDto.Parameter> parameters = new ArrayList<>();
        
        parameters.add(ToolDto.Parameter.builder()
            .name("datasourceType")
            .desc("数据源类型")
            .type("string")
            .required(true)
            .build());
        
        if ("database".equals(datasource.getDatasourceType())) {
            parameters.add(ToolDto.Parameter.builder()
                .name("sql")
                .desc("SQL查询语句")
                .type("string")
                .required(true)
                .build());
        }
        
        return ToolDto.builder()
            .id(datasource.getDatasourceId())
            .type("datasource")
            .name(datasource.getDatasourceName())
            .desc(StringUtils.hasText(datasource.getDescription()) ?
                datasource.getDescription() : "数据源: " + datasource.getDatasourceName())
            .parameters(parameters)
            .build();
    }
}