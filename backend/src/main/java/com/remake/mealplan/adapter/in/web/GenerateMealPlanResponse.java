package com.remake.mealplan.adapter.in.web;

import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanStatus;

/** 식단표 생성 접수 응답(202). 결과는 앱 안 알림으로 알린다. */
public record GenerateMealPlanResponse(Long mealPlanId, MealPlanStatus status) {

	static GenerateMealPlanResponse from(MealPlan mealPlan) {
		return new GenerateMealPlanResponse(mealPlan.id(), mealPlan.status());
	}
}
