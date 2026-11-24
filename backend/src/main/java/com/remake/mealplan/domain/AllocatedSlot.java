package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 이미 있는 칸이 쓰기로 한 재료. 다른 식단표의 오늘 이후·요리 안 한 칸만 넣는다
 * (지난 칸은 끝난 계획이고, 요리한 칸의 재료는 요리 완료 때 냉장고에서 이미 빠졌다).
 */
public record AllocatedSlot(LocalDate date, List<SlotIngredient> ingredients) {

	public AllocatedSlot {
		if (date == null) {
			throw new IllegalArgumentException("칸 날짜가 필요합니다.");
		}
		ingredients = ingredients == null ? List.of() : List.copyOf(ingredients);
	}
}
