package com.sparta.delivery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 후처리(AI 발송시한 알림) 실행 설정.
 *
 * <p>{@code @EnableAsync} 가 없으면 {@code @Async} 는 <b>에러 없이 조용히 무시되고</b>
 * 호출 스레드에서 그대로 동기 실행된다. 눈치채기 어려우니 반드시 함께 둔다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String DISPATCH_DEADLINE_EXECUTOR = "dispatchDeadlineExecutor";

    @Bean(name = DISPATCH_DEADLINE_EXECUTOR)
    public Executor dispatchDeadlineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-dispatch-");
        // 큐까지 가득 차면 호출 스레드에서 직접 실행한다. 알림을 버리는 것보다 낫다.
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
