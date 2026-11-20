package com.remake.ingredient.application.port.out;

/** 재료 삭제. 사용·폐기 기록이 있는 재료를 막는 규칙은 기록 테이블이 생기면 Service가 확인한다. */
public interface DeleteIngredientPort {

	void deleteById(Long ingredientId);
}
