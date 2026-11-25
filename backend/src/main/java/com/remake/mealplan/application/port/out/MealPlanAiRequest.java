package com.remake.mealplan.application.port.out;

import java.util.List;

import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.user.domain.AllergenCode;

/**
 * 식단표 AI 요청. 사용자가 쓴 글은 넣지 않는다(고른 값, DB의 냉장고 재료, 설정의 제외 재료 코드만).
 *
 * @param schedule               채울 날짜·끼니(남기는 칸 제외). 칸 교체는 그 칸 하나
 * @param ingredients            쓸 수 있는 냉장고 재료와 남은 양(다른 식단표에 배정된 양을 뺀 것)
 * @param excludedAllergenCodes  설정의 제외 재료(알레르기) 코드. 만든 레시피에 이 재료가 들어가면 안 된다. 없으면 빈 목록
 * @param avoidTitles            칸 교체에서 피할 요리 이름. 첫 번째가 바꾸기 전 레시피, 나머지는 같은 날 다른 끼니.
 *                               AI가 만든 레시피 제목이라 사용자가 쓴 글이 아니다. 생성은 빈 목록
 * @param avoidMainIngredientIds 칸 교체에서 주재료로 쓰지 않을 냉장고 재료 id(바꾸기 전 레시피가 쓰던 것). 생성은 빈 목록
 */
public record MealPlanAiRequest(MealPurpose purpose, int servings, MealSchedule schedule,
		List<FridgeStock> ingredients, List<AllergenCode> excludedAllergenCodes, List<String> avoidTitles,
		List<Long> avoidMainIngredientIds) {

	public MealPlanAiRequest {
		ingredients = List.copyOf(ingredients);
		excludedAllergenCodes = excludedAllergenCodes == null ? List.of() : List.copyOf(excludedAllergenCodes);
		avoidTitles = avoidTitles == null ? List.of() : List.copyOf(avoidTitles);
		avoidMainIngredientIds = avoidMainIngredientIds == null ? List.of() : List.copyOf(avoidMainIngredientIds);
	}

	/** 식단표 생성(피할 요리·재료 없음). */
	public MealPlanAiRequest(MealPurpose purpose, int servings, MealSchedule schedule, List<FridgeStock> ingredients,
			List<AllergenCode> excludedAllergenCodes) {
		this(purpose, servings, schedule, ingredients, excludedAllergenCodes, List.of(), List.of());
	}
}
