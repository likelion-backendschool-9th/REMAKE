package com.remake.recipe.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;

/**
 * 레시피 읽기. 재료는 화면 순서, 단계는 번호 순서로 채운다.
 */
public interface LoadRecipePort {

	/** 원본 작성자가 userId인 레시피만 조회한다. 없거나 다른 사용자의 레시피면 비어 있다. */
	Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId);

	/** 여러 레시피의 재료만 레시피 id별로(화면 순서) 읽는다. 다른 사용자의 레시피는 빠진다. */
	Map<Long, List<RecipeIngredient>> loadIngredientsByRecipeIds(Collection<Long> recipeIds, Long userId);

	/** 누가 만들었는지 보지 않는다. 볼 수 있는지(내가 만들었거나 저장했는지)는 부르는 쪽이 먼저 확인한다. */
	Optional<Recipe> loadById(Long recipeId);

	boolean existsByIdAndUserId(Long recipeId, Long userId);

	/** 없는 id는 빠진다. 볼 수 있는지는 부르는 쪽이 확인한다. */
	Map<Long, RecipeHeader> loadHeaders(Collection<Long> recipeIds);
}
