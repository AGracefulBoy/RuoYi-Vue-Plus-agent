package org.dromara.system.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.system.event.AgentCacheUpdateEvent;
import org.dromara.system.service.AgentLocalCacheService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 智能体缓存更新事件监听器
 * 监听智能体更新事件并执行相应的缓存操作
 *
 * @author system
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentCacheUpdateListener {
    
    private final AgentLocalCacheService agentLocalCacheService;
    
    /**
     * 处理智能体缓存更新事件
     *
     * @param event 缓存更新事件
     */
    @Async
    @EventListener
    public void handleAgentCacheUpdate(AgentCacheUpdateEvent event) {
        log.info("接收到智能体缓存更新事件 - 操作类型: {}, 智能体数量: {}", 
            event.getOperationType(), event.getAgentIds().size());
        
        try {
            switch (event.getOperationType()) {
                case UPDATE:
                case STATUS_CHANGE:
                    // 更新或状态变更时刷新缓存
                    event.getAgentIds().forEach(agentId -> {
                        log.debug("刷新智能体缓存 - agentId: {}", agentId);
                        agentLocalCacheService.refresh(agentId);
                    });
                    break;
                    
                case DELETE:
                    // 删除时失效缓存
                    log.debug("批量失效智能体缓存 - agentIds: {}", event.getAgentIds());
                    agentLocalCacheService.evictAgents(event.getAgentIds());
                    break;
                    
                case BATCH_REFRESH:
                    // 批量刷新
                    log.debug("批量刷新智能体缓存 - agentIds: {}", event.getAgentIds());
                    event.getAgentIds().forEach(agentLocalCacheService::refresh);
                    break;
                    
                default:
                    log.warn("未知的操作类型: {}", event.getOperationType());
            }
            
            log.info("智能体缓存更新完成 - 操作类型: {}", event.getOperationType());
            
        } catch (Exception e) {
            log.error("处理智能体缓存更新事件失败", e);
        }
    }
    
    /**
     * 获取缓存统计信息（可用于监控）
     */
    public void logCacheStats() {
        try {
            var stats = agentLocalCacheService.getCacheStats();
            log.info("缓存统计信息: {}", stats);
        } catch (Exception e) {
            log.error("获取缓存统计信息失败", e);
        }
    }
}