package com.remake.recipe.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import com.remake.recipe.application.port.in.GetRecipeUseCase.RecipeDetail;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeUnit;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 레시피 상세 모양(API 28·29·30). { id, title, servings, cookMinutes, tips, ingredients: [{ name, amount, unit }],
 * steps: [{ stepNo, content }], saved, favorite, shareable }.
 * 생성·다시 받기 직후에는 저장 전이라 saved·favorite가 false이고, 방금 만든 내 레시피라 shareable은 true다.
 */
public record RecipeDetailResponse(
		Long id,
		String title,
		int servings,
		int cookMinutes,
		@Schema(nullable = true) String tips,
		List<Ingredient> ingredients,
		List<Step> steps,
		boolean saved,
		boolean favorite,
		boolean shareable) {

	/** @param amount null이면 "약간" */
	@Schema(name = "RecipeDetailIngredient")
	public record Ingredient(String name, @Schema(nullable = true) BigDecimal amount, RecipeUnit unit) {
	}

	@Schema(name = "RecipeDetailStep")
	public record Step(int stepNo, String content) {
	}

	static RecipeDetailResponse from(RecipeDetail detail) {
		return of(detail.recipe(), detail.saved(), detail.favorite(), detail.shareable());
	}

	/** 방금 만든 레시피(생성·다시 받기). 아직 저장하지 않았다. */
	static RecipeDetailResponse from(Recipe recipe) {
		return of(recipe, false, false, true);
	}

	private static RecipeDetailResponse of(Recipe recipe, boolean saved, boolean favorite, boolean shareable) {
		List<String> steps = recipe.steps();
		return new RecipeDetailResponse(recipe.id(), recipe.title(), recipe.servings(), recipe.cookMinutes(),
				recipe.tips(),
				recipe.ingredients().stream().map(i -> new Ingredient(i.name(), i.amount(), i.unit())).toList(),
				IntStream.range(0, steps.size()).mapToObj(i -> new Step(i + 1, steps.get(i))).toList(),
				saved, favorite, shareable);
	}
}
