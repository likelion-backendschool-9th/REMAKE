package com.remake.mealplan.adapter.in.web;

import java.time.LocalDate;
import java.util.List;

import com.remake.mealplan.application.port.in.MealPlanDay;
import com.remake.mealplan.domain.MealType;

import io.swagger.v3.oas.annotations.media.Schema;

/** 날짜별 끼니 응답(API 40). */
public record MealPlanDayResponse(
		LocalDate date,
		@Schema(description = "그날의 칸(끼니 순: 아침, 점심, 저녁). 칸이 없으면 빈 목록") List<Slot> slots) {

	public record Slot(
			@Schema(description = "레시피 상세에서 요리 완료할 때 mealPlanSlotId로 보내는 값") Long slotId,
			MealType mealType,
			Long recipeId,
			String title,
			@Schema(description = "앞 3개 재료를 \" · \"로 이은 것", example = "닭가슴살 · 청상추 · 계란") String ingredientSummary,
			@Schema(description = "보여 주지 않은 재료 개수(화면이 +N, 0이면 생략)") int extraIngredientCount,
			@Schema(description = "저장했고 즐겨찾기를 켰으면 true") boolean favorite,
			@Schema(description = "이 칸에서 요리 완료했으면 true") boolean cooked) {
	}

	static MealPlanDayResponse from(MealPlanDay day) {
		return new MealPlanDayResponse(day.date(),
				day.slots().stream()
						.map(slot -> new Slot(slot.slotId(), slot.mealType(), slot.recipeId(), slot.title(),
								slot.ingredientSummary(), slot.extraIngredientCount(), slot.favorite(), slot.cooked()))
						.toList());
	}
}
