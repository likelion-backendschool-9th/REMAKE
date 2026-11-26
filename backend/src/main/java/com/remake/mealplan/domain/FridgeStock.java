package com.remake.mealplan.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;

/**
 * 식단표 계산에 쓰는 냉장고 재료 한 줄(냉장고 기능에서 읽은 값).
 *
 * @param categoryCode 그 냉장고 재료의 카테고리. 이 재료로 만든 레시피 줄의 카테고리가 된다(#91, 레시피 생성 #76과 같은 기준).
 *                     AI에는 보내지 않는다. 모르면 null
 */
public record FridgeStock(Long ingredientId, String name, BigDecimal amount, IngredientUnit unit,
		LocalDate expirationDate, IngredientCategoryCode categoryCode) {

	/** 카테고리를 모를 때(테스트 등). */
	public FridgeStock(Long ingredientId, String name, BigDecimal amount, IngredientUnit unit,
			LocalDate expirationDate) {
		this(ingredientId, name, amount, unit, expirationDate, null);
	}

	public FridgeStock {
		if (ingredientId == null || name == null || amount == null || unit == null || expirationDate == null) {
			throw new IllegalArgumentException("냉장고 재료의 id·이름·양·단위·소비기한이 필요합니다.");
		}
		if (amount.signum() < 0) {
			throw new IllegalArgumentException("냉장고 재료 양은 0 이상이어야 합니다.");
		}
	}
}
