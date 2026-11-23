package com.remake.mealplan.application.port.out;

import java.util.Optional;

import com.remake.mealplan.domain.MealPlan;

public interface LoadMealPlanPort {

	/** 회원의 식단표 중 GENERATING이 있는지. */
	boolean existsGenerating(Long userId);

	Optional<MealPlan> loadById(Long mealPlanId);
}
