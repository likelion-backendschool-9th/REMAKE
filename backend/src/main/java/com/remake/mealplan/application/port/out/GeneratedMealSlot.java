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
 * @param skipped     냉장고 재료가 떨어져 AI가 "못 만듦"으로 보낸 칸. 레시피가 없고, 일수 자르기는 이 칸이 있는 날에서 멈춘다
 */
public record GeneratedMealSlot(LocalDate date, MealType mealType, String title, int cookMinutes, String tips,
		List<SlotIngredient> ingredients, List<String> steps, boolean skipped) {

	public GeneratedMealSlot {
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
		steps = steps == null ? List.of() : List.copyOf(steps);
	}

	/** 레시피가 있는 칸. */
	public GeneratedMealSlot(LocalDate date, MealType mealType, String title, int cookMinutes, String tips,
			List<SlotIngredient> ingredients, List<String> steps) {
		this(date, mealType, title, cookMinutes, tips, ingredients, steps, false);
	}

	/** "못 만듦" 칸. */
	public static GeneratedMealSlot skipped(LocalDate date, MealType mealType) {
		return new GeneratedMealSlot(date, mealType, null, 0, null, List.of(), List.of(), true);
	}

	public MealSlotKey key() {
		return new MealSlotKey(date, mealType);
	}

	/** 일수 자르기에 쓰는 모양. */
	public PlannedSlot toPlannedSlot() {
		return new PlannedSlot(date, mealType, ingredients);
	}
}
