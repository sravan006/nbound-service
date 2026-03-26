package com.rxlink.inbound.router.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RouterExecutorConfig {

    @Bean(name = "routerTaskExecutor")
    public TaskExecutor routerTaskExecutor(
            @Value("${inbound.router.executor.core-pool-size:16}") int corePoolSize,
            @Value("${inbound.router.executor.max-pool-size:64}") int maxPoolSize,
            @Value("${inbound.router.executor.queue-capacity:1000}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("router-worker-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }
}
