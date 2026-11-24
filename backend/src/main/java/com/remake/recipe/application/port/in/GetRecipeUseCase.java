package com.remake.recipe.application.port.in;

import com.remake.recipe.domain.Recipe;

/**
 * 레시피 상세(API 30). 내가 만들었거나 저장 목록에 있는 레시피(저장한 공유 레시피 포함)만 볼 수 있다. 그 밖은 RECIPE_NOT_FOUND.
 */
public interface GetRecipeUseCase {

	/**
	 * @param saved    내 저장 목록에 있음
	 * @param favorite 저장했고 즐겨찾기를 켬
	 */
	record RecipeDetail(Recipe recipe, boolean saved, boolean favorite) {
	}

	RecipeDetail getRecipe(Long userId, Long recipeId);
}
