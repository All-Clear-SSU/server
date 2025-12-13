package opensource.project.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 및 스케줄링 설정
 *
 * 주요 기능:
 * 1. @Async 애노테이션을 사용한 비동기 메서드 실행 활성화
 * 2. @Scheduled 애노테이션을 사용한 스케줄링 활성화
 * 3. MQTT 메시지 버퍼링을 위한 스레드 풀 설정
 *
 * MQTT 메시지 처리 흐름:
 * - MQTT 메시지 수신 → bufferMessage() 비동기 호출 → 즉시 반환
 * - 별도 스레드에서 100ms마다 버퍼 처리 (@Scheduled)
 */
@Slf4j
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업을 위한 스레드 풀 설정
     *
     * @return ThreadPoolTaskExecutor
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 풀 크기 (센서 개수에 따라 조정 가능)
        executor.setCorePoolSize(5);

        // 최대 스레드 풀 크기
        executor.setMaxPoolSize(10);

        // 큐 용량 (대기 중인 작업 수)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 (로그에서 구분하기 쉽게)
        executor.setThreadNamePrefix("mqtt-async-");

        // 스레드 풀 초기화
        executor.initialize();

        log.info("비동기 처리 스레드 풀 설정 완료 - Core: {}, Max: {}, Queue: {}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity());

        return executor;
    }
}