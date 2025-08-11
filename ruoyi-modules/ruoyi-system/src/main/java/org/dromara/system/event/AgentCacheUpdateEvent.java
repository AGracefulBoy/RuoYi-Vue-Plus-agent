package org.dromara.system.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 智能体缓存更新事件
 * 用于通知缓存服务进行缓存刷新或失效
 *
 * @author system
 */
@Getter
public class AgentCacheUpdateEvent extends ApplicationEvent {
    
    /**
     * 操作类型
     */
    private final OperationType operationType;
    
    /**
     * 智能体ID列表
     */
    private final List<Long> agentIds;
    
    /**
     * 构造函数
     *
     * @param source 事件源
     * @param operationType 操作类型
     * @param agentIds 智能体ID列表
     */
    public AgentCacheUpdateEvent(Object source, OperationType operationType, List<Long> agentIds) {
        super(source);
        this.operationType = operationType;
        this.agentIds = agentIds;
    }
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        /**
         * 更新操作 - 刷新缓存
         */
        UPDATE,
        
        /**
         * 删除操作 - 失效缓存
         */
        DELETE,
        
        /**
         * 状态变更 - 刷新缓存
         */
        STATUS_CHANGE,
        
        /**
         * 批量刷新
         */
        BATCH_REFRESH
    }
}