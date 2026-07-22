package com.example.jobpuzzle.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    // PDF/OCR 추출처럼 오래 걸리는 작업을 요청 스레드와 분리해서 실행하기 위한 전용 풀
    @Bean(name = "documentExtractionExecutor")
    public Executor documentExtractionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-extract-");
        executor.initialize();
        return executor;
    }
}