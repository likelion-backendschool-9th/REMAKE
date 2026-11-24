package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.FridgeIngredient;

/** 재료 수정. 소비기한·보관 위치·가격을 고친다. */
public interface UpdateIngredientUseCase {

	/**
	 * 사용자 본인의 재료만 고친다. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND),
	 * 규칙을 어기면 BusinessException(INVALID_INPUT).
	 */
	FridgeIngredient updateIngredient(UpdateIngredientCommand command);
}
