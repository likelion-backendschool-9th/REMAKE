package com.remake.mealplan.application.port.in;

/**
 * 끊긴 생성 작업 정리. 배포 등으로 서버 작업이 사라져 GENERATING에 멈춘 식단표를 실패로 끝낸다
 * (그대로 두면 회원이 계속 MEAL_PLAN_IN_PROGRESS를 받고 알림도 오지 않는다).
 */
public interface FailAbandonedMealPlansUseCase {

	/** 처리한 식단표 수. 두 번 불러도 결과가 같다(이미 끝난 식단표는 건너뛴다). */
	int failAbandonedMealPlans();
}
