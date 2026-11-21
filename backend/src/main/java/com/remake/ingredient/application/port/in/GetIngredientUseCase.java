package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.Ingredient;

/** 재료 상세 조회. */
public interface GetIngredientUseCase {

	/** 사용자 본인의 재료. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND) */
	Ingredient getIngredient(Long userId, Long ingredientId);
}
