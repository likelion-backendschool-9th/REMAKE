package com.remake.cart.adapter.in.web;

import java.math.BigDecimal;

import com.remake.cart.application.port.in.GetCartItemsUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;

public record CartItemResponse(Long id, Long recipeId, String recipeTitle, String name,
		IngredientCategoryCode categoryCode, String imageUrl, BigDecimal amount, RecipeUnit unit) {
	static CartItemResponse from(GetCartItemsUseCase.Item item) {
		return new CartItemResponse(item.id(), item.recipeId(), item.recipeTitle(), item.name(),
				item.categoryCode(), item.imageUrl(), item.amount(), item.unit());
	}
}
