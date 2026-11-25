package com.remake.mealplan.adapter.in.web;

import com.remake.mealplan.application.port.in.ReplaceMealSlotUseCase.ReplacedMealSlot;

/**
 * API 42 응답. { slotId, recipeId, title, ingredientSummary, extraIngredientCount }
 * ingredientSummary는 앞 3개 재료를 " · "로 이은 것, extraIngredientCount는 나머지 개수("+N").
 */
public record ReplaceMealSlotResponse(Long slotId, Long recipeId, String title, String ingredientSummary,
		int extraIngredientCount) {

	static ReplaceMealSlotResponse from(ReplacedMealSlot slot) {
		return new ReplaceMealSlotResponse(slot.slotId(), slot.recipeId(), slot.title(), slot.ingredientSummary(),
				slot.extraIngredientCount());
	}
}
