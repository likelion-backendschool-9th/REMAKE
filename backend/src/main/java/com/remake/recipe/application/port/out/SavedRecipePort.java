package com.remake.recipe.application.port.out;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;

import com.remake.recipe.domain.SavedRecipe;
import com.remake.recipe.domain.SavedRecipeSort;

/** 저장한 레시피(saved_recipes) 읽기·쓰기. 모두 회원 본인의 저장만 다룬다. */
public interface SavedRecipePort {

	Optional<SavedRecipe> find(Long userId, Long recipeId);

	/** 아직 저장하지 않았으면 넣고 true. 이미 있으면(동시에 두 번 눌러도) 아무것도 바꾸지 않고 false. */
	boolean insertIfAbsent(Long userId, Long recipeId, LocalDateTime savedAt);

	void delete(Long userId, Long recipeId);

	void updateFavorite(Long userId, Long recipeId, boolean favorite);

	long countByUserId(Long userId);

	/**
	 * 저장 목록 한 쪽.
	 *
	 * @param favoriteOnly   즐겨찾기만
	 * @param availableNames null이면 거르지 않음. 있으면 물을 뺀 모든 재료 이름이 이 안에 있는 레시피만
	 */
	Page<SavedRecipe> findPage(Long userId, boolean favoriteOnly, Collection<String> availableNames,
			SavedRecipeSort sort, int page, int size);

	/** 저장한 것만 들어 있다. 값은 즐겨찾기 여부 */
	Map<Long, Boolean> findFavorites(Long userId, Collection<Long> recipeIds);
}
