package com.remake.mealplan.application.port.in;

/**
 * 요리 완료(API 32)에서 부른다. 오늘의 내 식단 칸에서 연 레시피로 요리했으면 그 칸을 요리 완료로 표시한다(다른 날 칸은 표시하지 않음).
 * 호출한 쪽(요리 차감)의 트랜잭션에 합류한다.
 */
public interface MarkMealSlotCookedUseCase {

	/**
	 * 내 식단 칸이 아니거나 칸의 레시피가 recipeId가 아니면 INVALID_INPUT.
	 * 오늘 칸이면 요리 시각을 남기고(이미 요리한 칸은 처음 시각 유지), 다른 날 칸이면 아무것도 바꾸지 않는다.
	 */
	void markCooked(Long userId, Long mealPlanSlotId, Long recipeId);
}
