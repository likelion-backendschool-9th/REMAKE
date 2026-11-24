package com.remake.recipe.application.port.in;

import java.util.Collection;
import java.util.Map;

/**
 * 장바구니 내부용 원본 제목 조회. 저장 취소 후에도 출처 제목을 표시한다.
 * 호출자는 본인 장바구니에서 읽은 recipeId만 전달한다. 웹에 직접 노출하지 않는다.
 * 없는 id는 결과에서 빠지며, 반환 Map의 순서는 보장하지 않는다.
 */
public interface GetCartRecipeTitlesUseCase {
	Map<Long, String> getTitles(Collection<Long> recipeIds);
}
