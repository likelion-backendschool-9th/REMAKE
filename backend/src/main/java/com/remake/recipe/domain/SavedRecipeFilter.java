package com.remake.recipe.domain;

/** 저장 목록(API 33) 필터 칩. 전체 · 즐겨찾기 · 바로 가능 */
public enum SavedRecipeFilter {
	ALL,
	FAVORITE,
	/**
	 * 지금 냉장고 재료로 만들 수 있는 레시피. 물을 뺀 모든 재료가 내 냉장고에 같은 이름으로 있어야 한다(소비기한 지난 재료는 없는 것으로 봄).
	 * 양은 아직 비교하지 않는다(단위 환산 #63 뒤에 더함). 레시피 영역 담당 결정.
	 */
	AVAILABLE
}
