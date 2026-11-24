package com.remake.recipe.application.port.out;

import java.util.List;

/**
 * 목록 한 줄에 필요한 레시피 값.
 *
 * @param ingredientNames 재료 이름. 레시피 재료 순서
 */
public record RecipeHeader(Long recipeId, Long userId, String title, List<String> ingredientNames) {

	public RecipeHeader {
		ingredientNames = List.copyOf(ingredientNames);
	}
}
