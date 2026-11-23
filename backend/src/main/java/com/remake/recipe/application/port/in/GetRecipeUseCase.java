package com.remake.recipe.application.port.in;

import com.remake.recipe.domain.Recipe;

/**
 * 레시피 상세(API 30). 지금은 내가 만든 레시피만 볼 수 있다. 다른 사람의 레시피는 RECIPE_NOT_FOUND.
 * 공유 레시피를 저장한 경우(저장 목록, #76)는 저장 기능이 생길 때 넓힌다.
 */
public interface GetRecipeUseCase {

	Recipe getRecipe(Long userId, Long recipeId);
}
