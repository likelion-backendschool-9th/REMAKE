package com.remake.mealplan.application.port.out;

import java.util.List;

import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.user.domain.AllergenCode;

/**
 * 식단표 AI 요청. 사용자가 쓴 글은 넣지 않는다(고른 값, DB의 냉장고 재료, 설정의 제외 재료 코드만).
 *
 * @param schedule              채울 날짜·끼니(남기는 칸 제외)
 * @param ingredients           쓸 수 있는 냉장고 재료와 남은 양(다른 식단표에 배정된 양을 뺀 것)
 * @param excludedAllergenCodes 설정의 제외 재료(알레르기) 코드. 만든 레시피에 이 재료가 들어가면 안 된다. 없으면 빈 목록
 */
public record MealPlanAiRequest(MealPurpose purpose, int servings, MealSchedule schedule,
		List<FridgeStock> ingredients, List<AllergenCode> excludedAllergenCodes) {

	public MealPlanAiRequest {
		ingredients = List.copyOf(ingredients);
		excludedAllergenCodes = excludedAllergenCodes == null ? List.of() : List.copyOf(excludedAllergenCodes);
	}
}
