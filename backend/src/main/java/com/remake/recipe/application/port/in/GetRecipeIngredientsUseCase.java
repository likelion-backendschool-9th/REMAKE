package com.remake.recipe.application.port.in;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 여러 레시피의 재료 줄 읽기. 식단표가 이미 있는 칸들의 재료 배정량을 계산할 때 쓴다. */
public interface GetRecipeIngredientsUseCase {

	/**
	 * 회원 본인의 레시피만 레시피 id별 재료 줄(화면 순서)로 돌려준다.
	 * 없거나 다른 회원의 레시피 id는 결과에 없다.
	 */
	Map<Long, List<RecipeIngredientLine>> getIngredients(Long userId, Collection<Long> recipeIds);
}
