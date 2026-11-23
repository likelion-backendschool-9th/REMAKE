package com.remake.recipe.application.port.in;

import com.remake.recipe.domain.Recipe;

/**
 * 레시피 생성(API 28). 성공하면 레시피를 저장하고 하루 횟수를 한 번 차감한다(같은 트랜잭션).
 * 실패(GENERATION_LIMIT_EXCEEDED, NO_INGREDIENTS, AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE)는 차감하지 않는다.
 */
public interface GenerateRecipeUseCase {

	Recipe generate(Long userId, GenerateRecipeCommand command);
}
