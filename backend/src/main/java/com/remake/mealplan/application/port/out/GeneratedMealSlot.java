package com.remake.mealplan.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.PlannedSlot;
import com.remake.mealplan.domain.SlotIngredient;

/**
 * AI가 만든 칸 하나와 그 레시피 전체. 인분은 식단표의 인원을 쓴다.
 *
 * @param ingredients 화면 순서. 냉장고 재료는 fridgeIngredientId가 있다
 * @param steps       조리 순서
 */
public record GeneratedMealSlot(LocalDate date, MealType mealType, String title, int cookMinutes, String tips,
		List<SlotIngredient> ingredients, List<String> steps) {

	public GeneratedMealSlot {
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
		steps = steps == null ? List.of() : List.copyOf(steps);
	}

	public MealSlotKey key() {
		return new MealSlotKey(date, mealType);
	}

	/** 일수 자르기에 쓰는 모양. */
	public PlannedSlot toPlannedSlot() {
		return new PlannedSlot(date, mealType, ingredients);
	}
}
