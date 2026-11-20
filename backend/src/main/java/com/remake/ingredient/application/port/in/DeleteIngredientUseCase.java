package com.remake.ingredient.application.port.in;

/** 재료 삭제. */
public interface DeleteIngredientUseCase {

	/** 사용자 본인의 재료만 지운다. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND) */
	void deleteIngredient(Long userId, Long ingredientId);
}
