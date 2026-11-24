package com.remake.recipe.application.port.in;

import org.springframework.data.domain.Page;

import com.remake.recipe.domain.SavedRecipeFilter;
import com.remake.recipe.domain.SavedRecipeSort;

/** 저장 목록(API 33). 저장이 0개이거나 필터 결과가 없으면 빈 목록이다. */
public interface GetSavedRecipesUseCase {

	/**
	 * @param totalCount 필터와 상관없는 전체 저장 수("저장한 레시피 N")
	 * @param items      필터·정렬을 적용한 한 쪽
	 */
	record SavedRecipes(long totalCount, Page<RecipeListItem> items) {
	}

	SavedRecipes getSavedRecipes(Long userId, SavedRecipeFilter filter, SavedRecipeSort sort, int page, int size);
}
