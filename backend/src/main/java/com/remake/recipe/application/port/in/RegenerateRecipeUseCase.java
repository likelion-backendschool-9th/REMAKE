package com.remake.recipe.application.port.in;

import com.remake.recipe.domain.Recipe;

/**
 * 다시 받기(API 29). 원래 레시피와 같은 조건(빠른·커스텀, 커스텀 선택값)으로 새 레시피를 만든다.
 * 언제든 누를 수 있고 생성과 같은 하루 횟수를 쓴다. 원래 레시피는 그대로 남는다.
 * 원래 레시피가 없거나 내 것이 아니면 RECIPE_NOT_FOUND. 실패 규칙은 {@link GenerateRecipeUseCase}와 같다.
 */
public interface RegenerateRecipeUseCase {

	Recipe regenerate(Long userId, Long recipeId);
}
