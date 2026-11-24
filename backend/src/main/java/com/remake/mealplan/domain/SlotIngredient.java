package com.remake.mealplan.domain;

import java.math.BigDecimal;

import com.remake.recipe.domain.RecipeUnit;

/**
 * 칸 레시피의 재료 한 줄(AI가 만든 것 또는 저장된 레시피의 것).
 *
 * @param fridgeIngredientId 이 재료로 쓰는 내 냉장고 재료 id. 냉장고에 없는 재료면 null
 * @param amount             양. null이면 "약간"
 */
public record SlotIngredient(Long fridgeIngredientId, String name, BigDecimal amount, RecipeUnit unit) {

	public SlotIngredient {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("재료 이름이 필요합니다.");
		}
		if (unit == null) {
			throw new IllegalArgumentException("재료 단위가 필요합니다.");
		}
		if (amount != null && amount.signum() <= 0) {
			throw new IllegalArgumentException("재료 양은 0보다 커야 합니다.");
		}
	}

	/** 양을 셀 수 있는 재료인지. 양이 없거나 꼬집·약간이면 배정·장바구니 계산에서 뺀다. */
	public boolean isMeasured() {
		return amount != null && unit != RecipeUnit.PINCH && unit != RecipeUnit.TO_TASTE;
	}

	/** 냉장고 재료와 이어진 재료인지. */
	public boolean usesFridge() {
		return fridgeIngredientId != null;
	}
}
