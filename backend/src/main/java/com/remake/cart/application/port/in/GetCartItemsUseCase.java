package com.remake.cart.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;

public interface GetCartItemsUseCase {
	List<Item> getItems(Long userId);

	record Item(Long id, Long recipeId, String recipeTitle, String name,
			IngredientCategoryCode categoryCode, String imageUrl, BigDecimal amount, RecipeUnit unit) { }
}
