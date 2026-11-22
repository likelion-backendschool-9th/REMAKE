package com.remake.recipe.application.port.out;

import java.util.Optional;

import com.remake.recipe.domain.Recipe;

/** 레시피 읽기. 사용자 본인의 레시피만 돌려주고, 없거나 다른 사용자의 레시피면 비어 있다. */
public interface LoadRecipePort {

	/** 재료는 화면 순서, 단계는 번호 순서로 채운다. */
	Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId);
}
