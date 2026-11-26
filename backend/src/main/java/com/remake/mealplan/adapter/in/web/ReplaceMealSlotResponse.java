package com.remake.mealplan.adapter.in.web;

import com.remake.mealplan.application.port.in.ReplaceMealSlotUseCase.ReplacedMealSlot;

/**
 * API 42 응답. { slotId, recipeId, title, ingredientSummary, extraIngredientCount, cartAddedCount }
 * ingredientSummary는 앞 3개 재료를 " · "로 이은 것, extraIngredientCount는 나머지 개수("+N").
 * cartAddedCount는 새 레시피의 부족 재료로 장바구니에 새로 넣은 행 수(#91). 1 이상이면 레시피 저장처럼 토스트를 띄운다.
 */
public record ReplaceMealSlotResponse(Long slotId, Long recipeId, String title, String ingredientSummary,
		int extraIngredientCount, int cartAddedCount) {

	static ReplaceMealSlotResponse from(ReplacedMealSlot slot) {
		return new ReplaceMealSlotResponse(slot.slotId(), slot.recipeId(), slot.title(), slot.ingredientSummary(),
				slot.extraIngredientCount(), slot.cartAddedCount());
	}
}
