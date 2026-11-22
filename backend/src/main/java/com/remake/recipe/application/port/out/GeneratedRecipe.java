package com.remake.recipe.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI가 돌려준 레시피 그대로의 값. 아직 검증하지 않았으므로 어떤 필드든 비어 있거나 틀릴 수 있다.
 * Service가 GeneratedRecipeValidator로 검증해 Recipe로 바꾼다.
 *
 * @param cookMinutes 조리 시간(분)
 */
public record GeneratedRecipe(
		String title,
		Integer cookMinutes,
		String tips,
		List<Ingredient> ingredients,
		List<String> steps) {

	/**
	 * 재료 한 줄.
	 *
	 * @param fridgeIngredientId 요청에 넣은 냉장고 재료 id 중 하나. 냉장고에 없는 재료면 null
	 * @param amount             양. 비어 있으면 "약간"
	 * @param unit               단위 코드 문자열(검증 전)
	 */
	public record Ingredient(Long fridgeIngredientId, String name, BigDecimal amount, String unit) {
	}
}
