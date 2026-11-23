package com.remake.mealplan.application.port.in;

import com.remake.mealplan.domain.MealPlan;

/** 식단표 생성 접수(API 38). 검사를 마치면 GENERATING으로 저장하고 바로 돌아온다. 생성은 서버 작업이 한다. */
public interface GenerateMealPlanUseCase {

	/**
	 * 확인 순서: 지난 시작일(PAST_DATE_NOT_ALLOWED) → 생성 중인 식단표(MEAL_PLAN_IN_PROGRESS)
	 * → 월 한도(MEAL_PLAN_LIMIT_EXCEEDED) → 겹침(MEAL_PLAN_OVERLAP, details.overlappingDates).
	 * 거절하면 아무것도 저장하지 않고 횟수도 차감하지 않는다.
	 *
	 * @return GENERATING 상태로 저장한 식단표
	 */
	MealPlan generate(GenerateMealPlanCommand command);
}
