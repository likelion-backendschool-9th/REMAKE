package com.remake.mealplan.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.IngredientUnit;

/** 식단표 계산에 쓰는 냉장고 재료 한 줄(냉장고 기능에서 읽은 값). */
public record FridgeStock(Long ingredientId, String name, BigDecimal amount, IngredientUnit unit,
		LocalDate expirationDate) {

	public FridgeStock {
		if (ingredientId == null || name == null || amount == null || unit == null || expirationDate == null) {
			throw new IllegalArgumentException("냉장고 재료의 id·이름·양·단위·소비기한이 필요합니다.");
		}
		if (amount.signum() < 0) {
			throw new IllegalArgumentException("냉장고 재료 양은 0 이상이어야 합니다.");
		}
	}
}
