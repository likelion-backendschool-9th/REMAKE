package com.remake.ingredient.application.port.out;

public interface DeleteIngredientsForUserPort {

	/** 재료 사용·폐기 기록과 냉장고 재료를 지운다. */
	void deleteAllByUserId(Long userId);
}
