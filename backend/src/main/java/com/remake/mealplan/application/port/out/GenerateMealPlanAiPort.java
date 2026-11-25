package com.remake.mealplan.application.port.out;

import java.util.List;

/** 식단표 칸 레시피를 AI로 만든다. 레시피 생성(04)과 포트를 따로 둔다(한 번에 여러 칸, #75). */
public interface GenerateMealPlanAiPort {

	/**
	 * 요청한 칸들의 레시피를 만든다. 일부 칸이 빠지거나 요청하지 않은 칸이 섞여 와도 된다(일수 자르기에서 거른다).
	 * 실패하면 BusinessException(AI_UPSTREAM_ERROR, AI_TIMEOUT, AI_INVALID_RESPONSE)을 던진다.
	 */
	List<GeneratedMealSlot> generate(MealPlanAiRequest request);
}
