package com.remake.recipe.adapter.in.web;

import com.remake.recipe.application.port.in.RecipeListItem;

/** 저장 목록 한 줄. ingredientSummary는 앞 3개 재료를 " · "로 이은 것, extraIngredientCount는 나머지 개수("+N") */
public record SavedRecipeItemResponse(Long recipeId, String title, String ingredientSummary,
		int extraIngredientCount, boolean favorite) {

	static SavedRecipeItemResponse from(RecipeListItem item) {
		return new SavedRecipeItemResponse(item.recipeId(), item.title(), item.ingredientSummary(),
				item.extraIngredientCount(), item.favorite());
	}
}
