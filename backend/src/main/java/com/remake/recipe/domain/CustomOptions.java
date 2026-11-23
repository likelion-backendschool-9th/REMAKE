package com.remake.recipe.domain;

import java.util.List;

/**
 * 커스텀 생성에서 고른 값. recipes.custom_options(jsonb)에 그대로 저장하고 "다시 받기"에 다시 쓴다.
 * 시트마다 하나만 고르고, 고르지 않은 항목은 null이다. 자유 입력은 없다.
 *
 * @param servings      식사 인원
 * @param ingredientIds 고른 내 냉장고 재료 id. 고르지 않았으면 빈 목록
 * @param allergenCodes 제외할 알레르기 칩 코드(19개, 설정 영역이 정의). 없으면 빈 목록
 */
public record CustomOptions(
		int servings,
		List<Long> ingredientIds,
		Taste taste,
		CookTime cookTime,
		CookingTool tool,
		CookingMethod method,
		Cuisine cuisine,
		List<String> allergenCodes) {

	public CustomOptions {
		if (servings <= 0) {
			throw new IllegalArgumentException("식사 인원은 1명 이상이어야 합니다.");
		}
		ingredientIds = ingredientIds == null ? List.of() : List.copyOf(ingredientIds);
		allergenCodes = allergenCodes == null ? List.of() : List.copyOf(allergenCodes);
	}
}
