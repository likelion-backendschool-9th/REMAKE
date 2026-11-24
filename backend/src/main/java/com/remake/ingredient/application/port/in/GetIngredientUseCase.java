package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.FridgeIngredient;

/** 재료 상세 조회. 카테고리와 오늘 기준 D-day·상태를 붙여 돌려준다. */
public interface GetIngredientUseCase {

	/** 사용자 본인의 재료. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND) */
	FridgeIngredient getIngredient(Long userId, Long ingredientId);
}
