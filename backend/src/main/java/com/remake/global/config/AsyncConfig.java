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
	public static final String MEAL_PLAN_TASK_EXECUTOR = "mealPlanTaskExecutor";

	/**
	 * 식단표 생성 서버 작업(API 38이 202로 답한 뒤 AI 호출·저장). 작업 하나가 AI를 기다리며 수십 초 걸리므로 이벤트와 나눈다.
	 * 동시에 도는 작업은 보통 2개다. ThreadPoolTaskExecutor는 스레드 2개가 바쁘면 새 작업을 대기열(50)에 넣고,
	 * 대기열이 가득 찬 뒤에야 스레드를 4개까지 늘린다. 회원당 생성 중인 식단표는 하나라 대기열 50 = 기다리는 회원 수다.
	 * 스레드와 대기열이 모두 차면(54개) MealPlanGenerationJobAdapter가 그 식단표를 바로 실패로 끝낸다.
	 * Gemini 과부하로 작업이 길어지면 대기열에서 기다린 작업이 끊긴 작업 정리(MealPlanAbandonedJobService, 접수 뒤 10분)에
	 * 걸릴 수 있다. 지금 규모에서는 대기가 거의 없어 그대로 둔다.
	 * 배포로 앱이 멈출 때 도는 작업을 25초까지 기다린다(컨테이너 stop_grace_period 70초 안). 그래도 끊긴 작업은 GENERATING으로 남는다.
	 */
	@Bean(name = MEAL_PLAN_TASK_EXECUTOR)
	public Executor mealPlanTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(50);
		executor.setThreadNamePrefix("meal-plan-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(25);
		executor.initialize();
		return executor;
	}

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
