package com.remake.mealplan.adapter.out.job;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.global.config.AsyncConfig;
import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.in.RunMealPlanGenerationUseCase;
import com.remake.mealplan.application.port.out.RequestMealPlanGenerationPort;

/**
 * 식단표 생성 작업을 식단표 작업 실행기에 넘긴다. 접수 트랜잭션이 커밋된 뒤에 넘겨서,
 * 작업이 아직 저장되지 않은 식단표를 읽거나 롤백된 접수로 작업이 도는 일이 없게 한다.
 * 실행기가 받지 못하면(대기열이 가득 참) 식단표를 바로 실패로 끝낸다. 그대로 두면 GENERATING에 멈춰 회원이 다시 만들 수 없다.
 */
@Component
class MealPlanGenerationJobAdapter implements RequestMealPlanGenerationPort {

	private final Executor executor;
	private final RunMealPlanGenerationUseCase runMealPlanGenerationUseCase;

	MealPlanGenerationJobAdapter(@Qualifier(AsyncConfig.MEAL_PLAN_TASK_EXECUTOR) Executor executor,
			RunMealPlanGenerationUseCase runMealPlanGenerationUseCase) {
		this.executor = executor;
		this.runMealPlanGenerationUseCase = runMealPlanGenerationUseCase;
	}

	@Override
	public void request(MealPlanGenerationJob job) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					submit(job);
				}
			});
			return;
		}
		submit(job);
	}

	private void submit(MealPlanGenerationJob job) {
		try {
			executor.execute(() -> runMealPlanGenerationUseCase.run(job));
		} catch (TaskRejectedException e) {
			runMealPlanGenerationUseCase.abandon(job);
		}
	}
}
