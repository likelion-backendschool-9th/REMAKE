package com.remake.mealplan.application.port.out;

import com.remake.mealplan.domain.MealPlan;

public interface SaveMealPlanPort {

	/**
	 * 새 식단표(GENERATING)를 저장하고 id가 채워진 식단표를 돌려준다.
	 * 같은 회원의 GENERATING이 이미 있으면(동시 요청) DB 유니크 인덱스가 막고 BusinessException(MEAL_PLAN_IN_PROGRESS)을 던진다.
	 */
	MealPlan saveNew(MealPlan mealPlan);

	/** 이미 저장된 식단표의 작업 결과(상태·만든 일수)를 반영한다. */
	void update(MealPlan mealPlan);
}
