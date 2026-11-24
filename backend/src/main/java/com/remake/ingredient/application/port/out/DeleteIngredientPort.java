package com.remake.ingredient.application.port.out;

/** 재료 삭제. 사용·폐기 기록이 있어도 지울 수 있고, 기록의 재료 연결은 DB가 비운다. */
public interface DeleteIngredientPort {

	void deleteById(Long ingredientId);
}
