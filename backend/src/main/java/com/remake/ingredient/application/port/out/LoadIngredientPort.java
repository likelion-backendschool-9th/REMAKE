package com.remake.ingredient.application.port.out;

import java.util.Optional;

import com.remake.ingredient.domain.Ingredient;

/** 재료 읽기. 사용자 본인의 재료만 돌려주고, 없거나 다른 사용자의 재료면 비어 있다. */
public interface LoadIngredientPort {

	Optional<Ingredient> loadByIdAndUserId(Long ingredientId, Long userId);
}
