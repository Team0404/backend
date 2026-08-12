package com.sparta.delivery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 후처리(AI 발송시한 알림) 실행 설정.
 *
 * <p>{@code @EnableAsync} 가 없으면 {@code @Async} 는 <b>에러 없이 조용히 무시되고</b>
 * 호출 스레드에서 그대로 동기 실행된다. 눈치채기 어려우니 반드시 함께 둔다.
 */
@Slf4j
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
        executor.setRejectedExecutionHandler(logAndDrop());
        executor.initialize();
        return executor;
    }

    /**
     * 큐까지 가득 찼을 때의 처리 정책.
     *
     * <p><b>{@code CallerRunsPolicy} 를 쓰지 않는다.</b> 이 이벤트는 트랜잭션 커밋 직후
     * ({@code AFTER_COMMIT}) 발행되므로, 그 시점의 호출 스레드는 <b>배송 생성(D1) 요청을
     * 처리 중인 톰캣 스레드</b>다. CallerRunsPolicy 를 쓰면 포화 상황에서 AI 호출 지연이
     * 그대로 D1 응답 시간으로 흘러들어가고, 톰캣 스레드까지 잠식해 배송 API 전체가 느려진다.
     * "AI 알림은 배송 성립의 전제가 아니다" 라는 설계 전제가 부하 상황에서만 조용히 깨지는 셈이라
     * 더 위험하다.
     *
     * <p>그래서 <b>알림을 버리고 ERROR 로 남긴다.</b> 버려진 건은 슬랙/AI 이력조차 생기지 않아
     * A2(실패 목록)/A4(재생성)로도 잡히지 않으므로, 이 로그가 유일한 추적 수단이다.
     * 로그가 쌓이기 시작하면 풀 크기를 늘리거나 메시지 큐 도입을 검토해야 한다는 신호다.
     */
    private ThreadPoolExecutor.AbortPolicy logAndDrop() {
        return new ThreadPoolExecutor.AbortPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.error("AI 발송시한 알림 작업이 거부되었다(스레드풀 포화). 이 알림은 발송되지 않는다. "
                                + "activeCount={}, queueSize={}, poolSize={}",
                        e.getActiveCount(), e.getQueue().size(), e.getPoolSize());
                // 예외를 던지지 않는다. AFTER_COMMIT 콜백으로 전파돼 봐야
                // 이미 커밋된 트랜잭션을 되돌릴 수 없고, 로그만 이중으로 남는다.
            }
        };
    }
}
