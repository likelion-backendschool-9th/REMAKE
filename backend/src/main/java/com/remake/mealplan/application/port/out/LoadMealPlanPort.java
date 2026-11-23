package com.remake.mealplan.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanStatus;

public interface LoadMealPlanPort {

	/** 회원의 식단표 중 GENERATING이 있는지. */
	boolean existsGenerating(Long userId);

	Optional<MealPlan> loadById(Long mealPlanId);

	/** 회원의 식단표 중 그 상태이고 시작일이 from~to(둘 다 포함)인 것. 시작일·id 순. */
	List<MealPlan> loadByStatusStartingBetween(Long userId, MealPlanStatus status, LocalDate from, LocalDate to);
}
