package com.remake.mealplan.application.port.in;

import java.time.LocalDate;
import java.util.List;

import com.remake.mealplan.domain.MealType;

/**
 * 날짜별 끼니(API 40).
 *
 * @param slots 그날의 칸(끼니 순: 아침, 점심, 저녁)
 */
public record MealPlanDay(LocalDate date, List<Slot> slots) {

	/**
	 * 칸 하나. 제목·재료 요약·즐겨찾기는 레시피 영역의 목록 한 줄을 그대로 쓴다.
	 *
	 * @param slotId 레시피 상세에서 요리 완료할 때 mealPlanSlotId로 보내는 값
	 * @param cooked 이 칸에서 요리 완료했으면 true
	 */
	public record Slot(Long slotId, MealType mealType, Long recipeId, String title, String ingredientSummary,
			int extraIngredientCount, boolean favorite, boolean cooked) {
	}
}
