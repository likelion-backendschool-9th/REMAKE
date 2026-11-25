package com.remake.recipe.application.port.in;

import java.util.Collection;
import java.util.Map;

import com.remake.recipe.domain.IngredientSummary;

/**
 * 공유 목록·랭킹(API 49·50)이 레시피 id 목록의 재료 요약을 한 번에 읽는다. 규칙은 {@link IngredientSummary#of}와 같다.
 * <ul>
 * <li><b>볼 수 있는지 확인하지 않는다.</b> 부르는 쪽이 숨기지 않은(VISIBLE) 공유 글이 가리키는 원본 레시피 id만 넘긴다.
 * 비공개 레시피 id를 넘기면 그 재료 이름이 그대로 나가므로 웹에 직접 노출하지 않는다.</li>
 * <li>결과의 순서는 입력 순서가 아니다. 부르는 쪽이 자기 목록 순서대로 {@code get}해서 맞춘다.</li>
 * <li>없는 id는 결과에 없다.</li>
 * </ul>
 */
public interface GetRecipeIngredientSummariesUseCase {

	Map<Long, IngredientSummary> getIngredientSummaries(Collection<Long> recipeIds);
}
