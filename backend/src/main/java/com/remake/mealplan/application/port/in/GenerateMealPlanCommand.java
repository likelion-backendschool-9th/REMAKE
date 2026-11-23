package com.remake.mealplan.application.port.in;

import java.time.LocalDate;
import java.util.Set;

import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealType;

/**
 * 식단표 생성 요청(API 38). 고른 값(날짜·enum·숫자)만 있고 사용자가 쓴 글은 없다.
 *
 * @param overwrite 기존 식단과 겹치는 날을 덮어쓸지. 겹치는데 false면 MEAL_PLAN_OVERLAP
 */
public record GenerateMealPlanCommand(Long userId, LocalDate startDate, MealPurpose purpose, Set<MealType> mealTypes,
		int servings, boolean overwrite) {
}
