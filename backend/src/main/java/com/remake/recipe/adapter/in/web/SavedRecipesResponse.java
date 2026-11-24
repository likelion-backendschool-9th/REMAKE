package com.remake.recipe.adapter.in.web;

import java.util.List;

import org.springframework.data.domain.Page;

import com.remake.recipe.application.port.in.GetSavedRecipesUseCase.SavedRecipes;
import com.remake.recipe.application.port.in.RecipeListItem;

/**
 * API 33 응답. { totalCount, content: [{ recipeId, title, ingredientSummary, extraIngredientCount, favorite }], page,
 * size, totalElements, hasNext }. totalCount는 필터와 상관없는 전체 저장 수, totalElements는 필터 결과 수다.
 */
public record SavedRecipesResponse(
		long totalCount,
		List<SavedRecipeItemResponse> content,
		int page,
		int size,
		long totalElements,
		boolean hasNext) {

	static SavedRecipesResponse from(SavedRecipes saved) {
		Page<RecipeListItem> items = saved.items();
		return new SavedRecipesResponse(saved.totalCount(),
				items.getContent().stream().map(SavedRecipeItemResponse::from).toList(), items.getNumber(),
				items.getSize(), items.getTotalElements(), items.hasNext());
	}
}
