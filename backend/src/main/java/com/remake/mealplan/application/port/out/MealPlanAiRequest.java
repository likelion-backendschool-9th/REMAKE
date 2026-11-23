package com.remake.mealplan.application.port.out;

import java.util.List;

import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealSchedule;

/**
 * 식단표 AI 요청. 사용자가 쓴 글은 넣지 않는다(고른 값과 DB의 냉장고 재료만).
 *
 * @param schedule    채울 날짜·끼니(남기는 칸 제외)
 * @param ingredients 쓸 수 있는 냉장고 재료와 남은 양(다른 식단표에 배정된 양을 뺀 것)
 */
public record MealPlanAiRequest(MealPurpose purpose, int servings, MealSchedule schedule,
		List<FridgeStock> ingredients) {

	public MealPlanAiRequest {
		ingredients = List.copyOf(ingredients);
	}
}
