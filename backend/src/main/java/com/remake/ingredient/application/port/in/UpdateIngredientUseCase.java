package com.remake.ingredient.application.port.in;

import com.remake.ingredient.domain.IngredientDetail;

/** 재료 수정. 소비기한·보관 위치·가격을 고친다. */
public interface UpdateIngredientUseCase {

	/**
	 * 사용자 본인의 재료만 고친다. 없거나 다른 사용자의 재료면 BusinessException(INGREDIENT_NOT_FOUND),
	 * 규칙을 어기면 BusinessException(INVALID_INPUT). 이름이나 카테고리가 실제로 바뀌면 단위를 서버가 다시 정하고 수량 숫자는 그대로 둔다.
	 */
	IngredientDetail updateIngredient(UpdateIngredientCommand command);
}
