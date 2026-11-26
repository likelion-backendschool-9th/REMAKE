package com.remake.receipt.application.port.out;

import java.time.LocalDateTime;

import com.remake.ingredient.domain.Ingredient;

public interface ManualIngredientExpensePort {

	void save(Ingredient ingredient, LocalDateTime registeredAt);

	/** 행이 없으면 0건 갱신으로 끝난다. {@code created_at}은 바꾸지 않는다. */
	void updatePrice(Long ingredientId, Long price);

	void deleteAllForUser(Long userId);
}
