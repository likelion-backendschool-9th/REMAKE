package com.remake.receipt.application.port.out;

import java.time.LocalDateTime;

import com.remake.ingredient.domain.Ingredient;

public interface ManualIngredientExpensePort {

	void save(Ingredient ingredient, LocalDateTime registeredAt);

	void deleteAllForUser(Long userId);
}
