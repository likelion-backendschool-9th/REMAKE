package com.remake.ingredient.application.port.out;

/** 사용·폐기 기록 읽기. */
public interface LoadIngredientLogPort {

	/** 이 재료에 사용·폐기 기록이 하나라도 있는지 */
	boolean existsByIngredientId(Long ingredientId);
}
