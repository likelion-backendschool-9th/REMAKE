package com.remake.receipt.application.port.in;

import java.time.LocalDateTime;

import com.remake.ingredient.domain.Ingredient;

public interface RecordManualIngredientExpenseUseCase {

	/** 영수증 없이 들어온 재료의 등록 이력을 같은 트랜잭션에서 저장한다. */
	void record(Ingredient ingredient, LocalDateTime registeredAt);
}
