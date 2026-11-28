package com.remake.mealplan.application.port.out;

import com.remake.mealplan.domain.MealPlanCombinationRules;

/** 식단표 재료 조합 검사 기준(#288 F). 앱이 켜질 때 한 번 읽은 값을 돌려준다. */
public interface LoadMealPlanCombinationRulesPort {

	MealPlanCombinationRules load();
}
