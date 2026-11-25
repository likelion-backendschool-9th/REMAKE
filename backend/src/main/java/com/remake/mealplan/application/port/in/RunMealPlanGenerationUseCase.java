package com.remake.mealplan.application.port.in;

/** 식단표 생성 서버 작업. 접수 트랜잭션이 커밋된 뒤 작업 실행기가 부른다. */
public interface RunMealPlanGenerationUseCase {

	/**
	 * 재료 준비 → AI → 일수 자르기 → 저장(한 트랜잭션: 덮어쓸 칸 삭제, 레시피·칸 저장, COMPLETED, 월 차감, 완성 알림).
	 * 실패하면 FAILED와 실패 알림만 남기고 기존 식단·횟수는 그대로다. 예외를 밖으로 던지지 않는다.
	 */
	void run(MealPlanGenerationJob job);

	/** 작업을 시작하지 못했을 때(실행기 대기열이 가득 참 등). 실패로 끝내고 실패 알림을 보낸다. */
	void abandon(MealPlanGenerationJob job);
}
