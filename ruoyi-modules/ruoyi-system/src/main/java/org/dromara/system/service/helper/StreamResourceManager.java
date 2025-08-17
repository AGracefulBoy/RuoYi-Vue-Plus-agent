package org.dromara.system.service.helper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 流资源管理器
 * 统一管理SSE流订阅，防止资源泄漏
 *
 * @author system
 */
@Slf4j
@Component
public class StreamResourceManager {

    /**
     * 活跃的流订阅
     */
    private final Map<String, StreamResource> activeSubscriptions = new ConcurrentHashMap<>();

    /**
     * 统计信息
     */
    private final AtomicInteger totalCreated = new AtomicInteger(0);
    private final AtomicInteger totalCleaned = new AtomicInteger(0);
    private final AtomicInteger totalTimeout = new AtomicInteger(0);

    /**
     * 配置
     */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10); // 默认超时时间
    private static final Duration WARNING_THRESHOLD = Duration.ofMinutes(5); // 警告阈值

    /**
     * 注册流订阅
     *
     * @param traceId      追踪ID
     * @param subscription 订阅对象
     */
    public void registerSubscription(String traceId, Disposable subscription) {
        registerSubscription(traceId, subscription, DEFAULT_TIMEOUT);
    }

    /**
     * 注册流订阅（带自定义超时）
     *
     * @param traceId      追踪ID
     * @param subscription 订阅对象
     * @param timeout      超时时间
     */
    public void registerSubscription(String traceId, Disposable subscription, Duration timeout) {
        if (traceId == null || subscription == null) {
            log.warn("尝试注册空的订阅，追踪ID: {}", traceId);
            return;
        }

        StreamResource resource = new StreamResource(subscription, timeout);
        StreamResource old = activeSubscriptions.put(traceId, resource);
        
        // 如果存在旧的订阅，先清理
        if (old != null) {
            cleanupResource(old);
            log.warn("覆盖已存在的订阅，追踪ID: {}", traceId);
        }
        
        totalCreated.incrementAndGet();
        log.debug("注册流订阅 - 追踪ID: {}, 超时: {}秒, 当前活跃数: {}", 
            traceId, timeout.getSeconds(), activeSubscriptions.size());
    }

    /**
     * 手动清理资源
     *
     * @param traceId 追踪ID
     * @return 是否成功清理
     */
    public boolean cleanup(String traceId) {
        StreamResource resource = activeSubscriptions.remove(traceId);
        if (resource != null) {
            boolean success = cleanupResource(resource);
            if (success) {
                totalCleaned.incrementAndGet();
                log.info("清理流订阅成功 - 追踪ID: {}, 剩余活跃数: {}", 
                    traceId, activeSubscriptions.size());
            }
            return success;
        }
        return false;
    }

    /**
     * 清理资源
     */
    private boolean cleanupResource(StreamResource resource) {
        try {
            Disposable subscription = resource.getSubscription();
            if (subscription != null && !subscription.isDisposed()) {
                subscription.dispose();
                log.debug("已释放流订阅资源");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("清理流订阅时发生错误", e);
            return false;
        }
    }

    /**
     * 检查订阅是否活跃
     *
     * @param traceId 追踪ID
     * @return 是否活跃
     */
    public boolean isActive(String traceId) {
        StreamResource resource = activeSubscriptions.get(traceId);
        if (resource == null) {
            return false;
        }
        
        Disposable subscription = resource.getSubscription();
        return subscription != null && !subscription.isDisposed();
    }

    /**
     * 获取活跃订阅数量
     */
    public int getActiveCount() {
        return activeSubscriptions.size();
    }

    /**
     * 定期清理超时的订阅（每分钟执行）
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void cleanupTimeoutSubscriptions() {
        log.debug("开始清理超时的流订阅...");
        
        int cleaned = 0;
        Instant now = Instant.now();
        
        for (Map.Entry<String, StreamResource> entry : activeSubscriptions.entrySet()) {
            String traceId = entry.getKey();
            StreamResource resource = entry.getValue();
            
            // 检查是否超时
            if (resource.isTimeout(now)) {
                if (cleanup(traceId)) {
                    cleaned++;
                    totalTimeout.incrementAndGet();
                    log.warn("清理超时的流订阅 - 追踪ID: {}, 创建时间: {}, 超时: {}秒", 
                        traceId, resource.getCreateTime(), resource.getTimeout().getSeconds());
                }
            } else if (resource.shouldWarn(now)) {
                // 发出警告
                log.warn("流订阅即将超时 - 追踪ID: {}, 已运行: {}秒", 
                    traceId, Duration.between(resource.getCreateTime(), now).getSeconds());
            }
        }
        
        if (cleaned > 0) {
            log.info("清理完成，清理数量: {}, 剩余活跃数: {}", cleaned, activeSubscriptions.size());
        }
        
        // 打印统计信息
        logStatistics();
    }

    /**
     * 清理所有订阅
     */
    public void cleanupAll() {
        log.info("开始清理所有流订阅，当前数量: {}", activeSubscriptions.size());
        
        int cleaned = 0;
        for (String traceId : activeSubscriptions.keySet()) {
            if (cleanup(traceId)) {
                cleaned++;
            }
        }
        
        log.info("清理所有流订阅完成，清理数量: {}", cleaned);
    }

    /**
     * 获取统计信息
     */
    public ResourceStatistics getStatistics() {
        ResourceStatistics stats = new ResourceStatistics();
        stats.setActiveCount(activeSubscriptions.size());
        stats.setTotalCreated(totalCreated.get());
        stats.setTotalCleaned(totalCleaned.get());
        stats.setTotalTimeout(totalTimeout.get());
        
        // 计算平均存活时间
        Instant now = Instant.now();
        long totalAliveSeconds = 0;
        int count = 0;
        
        for (StreamResource resource : activeSubscriptions.values()) {
            Duration aliveTime = Duration.between(resource.getCreateTime(), now);
            totalAliveSeconds += aliveTime.getSeconds();
            count++;
        }
        
        if (count > 0) {
            stats.setAverageAliveSeconds(totalAliveSeconds / count);
        }
        
        return stats;
    }

    /**
     * 打印统计信息
     */
    private void logStatistics() {
        if (log.isDebugEnabled()) {
            ResourceStatistics stats = getStatistics();
            log.debug("流资源统计 - 活跃: {}, 总创建: {}, 总清理: {}, 超时清理: {}, 平均存活: {}秒",
                stats.getActiveCount(),
                stats.getTotalCreated(),
                stats.getTotalCleaned(),
                stats.getTotalTimeout(),
                stats.getAverageAliveSeconds());
        }
    }

    /**
     * 流资源包装类
     */
    private static class StreamResource {
        private final Disposable subscription;
        private final Instant createTime;
        private final Duration timeout;

        public StreamResource(Disposable subscription, Duration timeout) {
            this.subscription = subscription;
            this.createTime = Instant.now();
            this.timeout = timeout;
        }

        public Disposable getSubscription() {
            return subscription;
        }

        public Instant getCreateTime() {
            return createTime;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public boolean isTimeout(Instant now) {
            Duration aliveTime = Duration.between(createTime, now);
            return aliveTime.compareTo(timeout) > 0;
        }

        public boolean shouldWarn(Instant now) {
            Duration aliveTime = Duration.between(createTime, now);
            // 当存活时间超过超时时间的一半时发出警告
            return aliveTime.compareTo(timeout.dividedBy(2)) > 0;
        }
    }

    /**
     * 资源统计信息
     */
    public static class ResourceStatistics {
        private int activeCount;
        private int totalCreated;
        private int totalCleaned;
        private int totalTimeout;
        private long averageAliveSeconds;

        // Getters and setters
        public int getActiveCount() {
            return activeCount;
        }

        public void setActiveCount(int activeCount) {
            this.activeCount = activeCount;
        }

        public int getTotalCreated() {
            return totalCreated;
        }

        public void setTotalCreated(int totalCreated) {
            this.totalCreated = totalCreated;
        }

        public int getTotalCleaned() {
            return totalCleaned;
        }

        public void setTotalCleaned(int totalCleaned) {
            this.totalCleaned = totalCleaned;
        }

        public int getTotalTimeout() {
            return totalTimeout;
        }

        public void setTotalTimeout(int totalTimeout) {
            this.totalTimeout = totalTimeout;
        }

        public long getAverageAliveSeconds() {
            return averageAliveSeconds;
        }

        public void setAverageAliveSeconds(long averageAliveSeconds) {
            this.averageAliveSeconds = averageAliveSeconds;
        }
    }

    /**
     * 在应用关闭时清理所有资源
     */
    @jakarta.annotation.PreDestroy
    public void destroy() {
        log.info("应用关闭，清理所有流资源...");
        cleanupAll();
    }
}