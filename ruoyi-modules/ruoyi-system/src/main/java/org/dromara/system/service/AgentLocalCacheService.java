package org.dromara.system.service;

import org.dromara.system.domain.SysAgent;
import org.dromara.system.domain.dto.ToolDto;

import java.util.List;
import java.util.Map;

/**
 * 智能体本地缓存服务接口
 * 基于Caffeine实现的本地内存缓存，提供快速访问和实时失效机制
 *
 * @author system
 */
public interface AgentLocalCacheService {
    
    /**
     * 获取智能体信息（带缓存）
     *
     * @param agentId 智能体ID
     * @return 智能体信息
     */
    SysAgent getAgent(Long agentId);
    
    /**
     * 获取智能体可用工具列表（带缓存）
     *
     * @param agentId 智能体ID
     * @return 工具DTO列表
     */
    List<ToolDto> getAvailableTools(Long agentId);
    
    /**
     * 缓存智能体信息
     *
     * @param agent 智能体信息
     */
    void cacheAgent(SysAgent agent);
    
    /**
     * 缓存智能体工具列表
     *
     * @param agentId 智能体ID
     * @param tools 工具列表
     */
    void cacheAvailableTools(Long agentId, List<ToolDto> tools);
    
    /**
     * 清除指定智能体的所有缓存
     *
     * @param agentId 智能体ID
     */
    void evictAgent(Long agentId);
    
    /**
     * 清除所有智能体缓存
     */
    void evictAll();
    
    /**
     * 批量清除智能体缓存
     *
     * @param agentIds 智能体ID列表
     */
    void evictAgents(List<Long> agentIds);
    
    /**
     * 获取缓存统计信息
     *
     * @return 缓存统计信息
     */
    Map<String, Object> getCacheStats();
    
    /**
     * 预热缓存（加载活跃智能体）
     *
     * @param agentIds 需要预热的智能体ID列表
     */
    void warmUp(List<Long> agentIds);
    
    /**
     * 刷新指定智能体的缓存
     *
     * @param agentId 智能体ID
     */
    void refresh(Long agentId);
}