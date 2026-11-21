package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.Ingredient;

/** 재료 수정. 소비기한·보관 위치·가격을 고친다. */
public interface UpdateIngredientUseCase {

	/**
	 * 사용자 본인의 재료만 고친다. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND),
	 * 규칙을 어기면 BusinessException(INVALID_INPUT). 소비기한을 고치면 추정 표시가 꺼진다.
	 */
	Ingredient updateIngredient(UpdateIngredientCommand command);
}
