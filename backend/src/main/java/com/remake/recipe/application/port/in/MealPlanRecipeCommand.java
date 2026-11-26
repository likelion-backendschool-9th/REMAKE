package com.remake.recipe.application.port.in;

import java.math.BigDecimal;
import java.util.List;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;

/**
 * 식단표 칸에 넣을 레시피 하나(식단표 생성 작업이 AI로 만든 것). 규칙은 Recipe.create와 같다.
 *
 * @param ingredients 화면 순서
 * @param steps       조리 순서(1번부터)
 */
public record MealPlanRecipeCommand(String title, int servings, int cookMinutes, String tips,
		List<Ingredient> ingredients, List<String> steps) {

	/**
	 * @param ingredientId 이 재료로 쓰는 내 냉장고 재료 id(요리 완료 차감 대상). 냉장고에 없는 재료면 null
	 * @param amount       양. null이면 "약간"
	 * @param categoryCode 식단표가 정한 재료 카테고리(#91, 사전 → 냉장고 재료 → AI 순서). recipe_ingredients.category_code로 저장한다. 모르면 null
	 */
	public record Ingredient(Long ingredientId, String name, BigDecimal amount, RecipeUnit unit,
			IngredientCategoryCode categoryCode) {

		/** 카테고리를 모를 때. */
		public Ingredient(Long ingredientId, String name, BigDecimal amount, RecipeUnit unit) {
			this(ingredientId, name, amount, unit, null);
		}
	}
}
