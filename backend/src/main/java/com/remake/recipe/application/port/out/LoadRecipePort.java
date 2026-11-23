package com.remake.recipe.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;

/** 레시피 읽기. 사용자 본인의 레시피만 돌려주고, 없거나 다른 사용자의 레시피면 비어 있다. */
public interface LoadRecipePort {

	/** 재료는 화면 순서, 단계는 번호 순서로 채운다. */
	Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId);

	/** 여러 레시피의 재료만 레시피 id별로(화면 순서) 읽는다. 다른 사용자의 레시피는 빠진다. */
	Map<Long, List<RecipeIngredient>> loadIngredientsByRecipeIds(Collection<Long> recipeIds, Long userId);
}
