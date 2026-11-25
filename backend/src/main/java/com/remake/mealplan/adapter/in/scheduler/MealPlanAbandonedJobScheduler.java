package com.remake.mealplan.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.mealplan.application.port.in.FailAbandonedMealPlansUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 1분마다 끊긴 식단표 생성 작업을 정리한다. 블루·그린 배포 중 두 컨테이너가 같이 돌려도
 * 식단표 행을 잠그고 다시 확인하므로 한 번만 처리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class MealPlanAbandonedJobScheduler {

	private final FailAbandonedMealPlansUseCase failAbandonedMealPlansUseCase;

	@Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
	void failAbandonedMealPlans() {
		int failed = failAbandonedMealPlansUseCase.failAbandonedMealPlans();
		if (failed > 0) {
			log.info("끊긴 식단표 생성 작업 {}개를 실패로 끝냄", failed);
		}
	}
}
