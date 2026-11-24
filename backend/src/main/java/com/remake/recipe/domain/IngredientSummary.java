package com.remake.recipe.domain;

import java.util.List;

/**
 * 목록 한 줄의 재료 요약. 저장 목록(API 33), 식단표 하루 조회·칸 교체(API 40·42), 공유 레시피(API 49·50)가 같은 규칙을 쓴다.
 * 레시피 재료 순서대로 앞 {@value #SHOWN}개 이름을 " · "로 잇고, 나머지 개수를 따로 준다(화면이 "+N"으로 표시, 0이면 생략).
 * 물·꼬집·약간도 재료 줄이면 그대로 센다(상세의 재료 칩 개수와 맞춤). 피그마 p4.1·p4.1.4 기준, 레시피 영역 담당 결정.
 *
 * @param summary    예: "닭가슴살 · 청상추 · 계란". 재료가 없으면 빈 문자열
 * @param extraCount 보여 주지 않은 재료 개수
 */
public record IngredientSummary(String summary, int extraCount) {

	public static final int SHOWN = 3;
	public static final String SEPARATOR = " · ";

	/** @param names 재료 이름. 레시피 재료 순서(sort_order) */
	public static IngredientSummary of(List<String> names) {
		List<String> shown = names.subList(0, Math.min(SHOWN, names.size()));
		return new IngredientSummary(String.join(SEPARATOR, shown), names.size() - shown.size());
	}
}
