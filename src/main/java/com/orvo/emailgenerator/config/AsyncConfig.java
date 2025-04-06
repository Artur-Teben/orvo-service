package com.orvo.emailgenerator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "leadVerificationExecutor")
    public Executor leadVerificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // start threads
        executor.setMaxPoolSize(20); // max threads
        executor.setQueueCapacity(100); // task queue size
        executor.setThreadNamePrefix("LeadVerifier-");
        executor.initialize();
        return executor;
    }
}

