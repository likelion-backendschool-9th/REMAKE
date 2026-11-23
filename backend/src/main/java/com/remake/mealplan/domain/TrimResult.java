package com.remake.mealplan.domain;

import java.util.List;

/**
 * 일수 자르기 결과.
 *
 * @param generatedDays 시작일부터 만든 일수. 0이면 하루도 못 만든 것(MEAL_PLAN_NO_INGREDIENTS)
 * @param shortages     남긴 날의 칸에서 냉장고에 없는 재료(장바구니에 담을 것). 칸마다 따로, 합치지 않는다
 */
public record TrimResult(int generatedDays, List<Shortage> shortages) {

	public TrimResult {
		if (generatedDays < 0) {
			throw new IllegalArgumentException("만든 일수는 0 이상이어야 합니다.");
		}
		shortages = List.copyOf(shortages);
	}

	public boolean hasNoDays() {
		return generatedDays == 0;
	}

	/** 장바구니에 담을 재료 한 줄과 그 재료를 쓰는 칸. */
	public record Shortage(MealSlotKey slot, SlotIngredient ingredient) {
	}
}
