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

    // AI 분석은 한 건에 수십 초~수 분 걸리므로 HTTP 요청 스레드가 아닌 전용 풀에서 실행한다.
    // 동시에 도는 분석 수를 묶어 AI 제공자 요청 한도와 DB 연결을 보호하고, 대기열이 차면 접수를 거절한다.
    @Bean(name = "analysisExecutor")
    public ThreadPoolTaskExecutor analysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("analysis-run-");
        executor.initialize();
        return executor;
    }
}