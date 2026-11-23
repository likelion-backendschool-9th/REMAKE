package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.util.List;

/** AI가 만든 칸 하나와 그 레시피 재료. */
public record PlannedSlot(LocalDate date, MealType mealType, List<SlotIngredient> ingredients) {

	public PlannedSlot {
		if (date == null || mealType == null) {
			throw new IllegalArgumentException("칸에는 날짜와 끼니가 필요합니다.");
		}
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
	}

	public MealSlotKey key() {
		return new MealSlotKey(date, mealType);
	}
}
