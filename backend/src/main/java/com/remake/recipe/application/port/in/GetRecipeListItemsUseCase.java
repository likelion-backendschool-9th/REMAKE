package com.remake.recipe.application.port.in;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 영역이 목록 한 줄(제목·재료 요약·즐겨찾기)을 레시피 id로 한 번에 읽는다. 식단표 하루 조회(API 40)·칸 교체(API 42)가 쓴다.
 * 내가 만들었거나 저장한 레시피만 돌려주고, 그 밖의 id는 결과에서 빠진다.
 * <ul>
 * <li>결과의 순서는 입력 id 순서가 아니다. 화면 순서는 부르는 쪽이 자기 목록 순서대로 {@code get}해서 맞춘다.</li>
 * <li>입력 id가 결과에 없으면 없는 레시피이거나 이 사용자가 볼 수 없는 레시피다(구분하지 않는다).</li>
 * </ul>
 */
public interface GetRecipeListItemsUseCase {

	Map<Long, RecipeListItem> getListItems(Long userId, Collection<Long> recipeIds);
}
