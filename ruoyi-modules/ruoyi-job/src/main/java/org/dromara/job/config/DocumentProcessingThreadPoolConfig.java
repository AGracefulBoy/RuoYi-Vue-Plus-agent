package org.dromara.job.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 文档处理线程池配置
 * 为文档解析、分块、向量化等任务提供独立的线程池
 */
@Configuration
public class DocumentProcessingThreadPoolConfig {

    @Value("${document.processing.thread-pool.core-size:10}")
    private int corePoolSize;

    @Value("${document.processing.thread-pool.max-size:20}")
    private int maxPoolSize;

    @Value("${document.processing.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    @Value("${document.processing.embedding.concurrent-docs:5}")
    private int embeddingConcurrentDocs;

    /**
     * 文档处理通用线程池
     * 用于文档解析、URL同步、文档分块等任务
     */
    @Bean("documentProcessingExecutor")
    public ThreadPoolTaskExecutor documentProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("doc-process-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(1000);
        executor.initialize();
        return executor;
    }

    /**
     * 向量化专用线程池
     * 控制向量化并发数，避免过多并发导致API限流或内存溢出
     */
    @Bean("embeddingExecutor")
    public ThreadPoolTaskExecutor embeddingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(embeddingConcurrentDocs);
        executor.setMaxPoolSize(embeddingConcurrentDocs * 2);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("embedding-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
