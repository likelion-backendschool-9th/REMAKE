package com.remake.mealplan.application.port.in;

/**
 * 식단 칸 레시피 교체(API 42). 그 칸의 레시피를 새로 만들어 바꾸고, 성공할 때만 레시피 하루 횟수를 1회 쓴다.
 * 실패하면 기존 레시피와 남은 횟수가 그대로이고, 식단표 월 한도는 쓰지 않는다.
 */
public interface ReplaceMealSlotUseCase {

	/**
	 * 거절: 없거나 다른 사용자의 칸 SLOT_NOT_FOUND, 지난 날짜 PAST_DATE_NOT_ALLOWED, 하루 횟수 없음 GENERATION_LIMIT_EXCEEDED,
	 * 쓸 수 있는 냉장고 재료 없음 MEAL_PLAN_NO_INGREDIENTS, AI 실패 AI_UPSTREAM_ERROR·AI_TIMEOUT·AI_INVALID_RESPONSE(1회 재시도 뒤).
	 * 오늘 요리한 칸도 바꿀 수 있고, 바꾸면 요리 완료 표시가 지워진다.
	 */
	ReplacedMealSlot replace(Long userId, Long slotId);

	/**
	 * 바뀐 칸. 재료 요약은 목록 한 줄과 같은 규칙(recipe IngredientSummary)이다.
	 *
	 * @param ingredientSummary    앞 3개 재료를 " · "로 이은 것
	 * @param extraIngredientCount 보여 주지 않은 재료 개수
	 */
	record ReplacedMealSlot(Long slotId, Long recipeId, String title, String ingredientSummary,
			int extraIngredientCount) {
	}
}
