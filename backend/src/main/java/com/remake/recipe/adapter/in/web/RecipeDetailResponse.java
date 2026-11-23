package com.remake.recipe.adapter.in.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeUnit;

/**
 * 레시피 상세 모양(API 28·29·30). { id, title, servings, cookMinutes, tips, ingredients: [{ name, amount, unit }],
 * steps: [{ stepNo, content }], saved, favorite, shareable }.
 * 저장·즐겨찾기(#76)와 공유(#7 영역)가 생기기 전까지 saved·favorite는 false, shareable은 true다.
 */
public record RecipeDetailResponse(
		Long id,
		String title,
		int servings,
		int cookMinutes,
		String tips,
		List<Ingredient> ingredients,
		List<Step> steps,
		boolean saved,
		boolean favorite,
		boolean shareable) {

	/** @param amount null이면 "약간" */
	public record Ingredient(String name, BigDecimal amount, RecipeUnit unit) {
	}

	public record Step(int stepNo, String content) {
	}

	static RecipeDetailResponse from(Recipe recipe) {
		List<String> steps = recipe.steps();
		return new RecipeDetailResponse(recipe.id(), recipe.title(), recipe.servings(), recipe.cookMinutes(),
				recipe.tips(),
				recipe.ingredients().stream().map(i -> new Ingredient(i.name(), i.amount(), i.unit())).toList(),
				IntStream.range(0, steps.size()).mapToObj(i -> new Step(i + 1, steps.get(i))).toList(),
				false, false, true);
	}
}
