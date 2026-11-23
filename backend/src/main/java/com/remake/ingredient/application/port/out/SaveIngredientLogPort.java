package com.remake.ingredient.application.port.out;

import com.remake.ingredient.domain.IngredientLog;

/** 사용·폐기 기록 저장. 기록은 새로 쌓기만 하고 고치지 않는다. */
public interface SaveIngredientLogPort {

	IngredientLog save(IngredientLog log);
}
