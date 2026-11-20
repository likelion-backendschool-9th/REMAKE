package com.remake.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 이벤트 적재 등 요청을 막지 않는 비동기 작업용 실행기.
 * 테스트는 이 빈을 그대로 쓰고, 종료 전 대기만 IntegrationTestSupport에서 한다(SyncTaskExecutor로 바꾸지 않는다).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

	public static final String EVENT_TASK_EXECUTOR = "eventTaskExecutor";

	@Bean(name = EVENT_TASK_EXECUTOR)
	public Executor eventTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("event-");
		executor.initialize();
		return executor;
	}
}
