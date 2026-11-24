package com.remake.recipe.application.port.in;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 영역이 목록 한 줄(제목·재료 요약·즐겨찾기)을 레시피 id로 한 번에 읽는다. 식단표 하루 조회(API 40)·칸 교체(API 42)가 쓴다.
 * 내가 만들었거나 저장한 레시피만 돌려주고, 그 밖의 id는 결과에서 빠진다.
 */
public interface GetRecipeListItemsUseCase {

	Map<Long, RecipeListItem> getListItems(Long userId, Collection<Long> recipeIds);
}
