package com.remake.cart.domain;

import java.math.BigDecimal;

import com.remake.recipe.domain.RecipeUnit;

/** 장바구니 수량 규칙. 다른 레시피의 항목은 합치지 않고 선택한 행의 잔량만 바꾼다. */
public record CartItem(Long id, Long userId, Long recipeId, Long mealPlanSlotId, Long categoryId,
		String name, BigDecimal amount, RecipeUnit unit) {

	public static void validateAmount(BigDecimal value) {
		if (value == null || value.signum() <= 0) {
			throw new IllegalArgumentException("수량은 0보다 커야 합니다.");
		}
		BigDecimal stripped = value.stripTrailingZeros();
		if (stripped.scale() > 3 || stripped.precision() - stripped.scale() > 7) {
			throw new IllegalArgumentException("수량은 정수 7자리, 소수 3자리 이내여야 합니다.");
		}
	}

	public CartItem subtract(BigDecimal deduction) {
		validateAmount(deduction);
		if (deduction.compareTo(amount) > 0) {
			throw new IllegalArgumentException("장바구니 잔량보다 많이 뺄 수 없습니다.");
		}
		return new CartItem(id, userId, recipeId, mealPlanSlotId, categoryId, name,
				amount.subtract(deduction), unit);
	}
}
