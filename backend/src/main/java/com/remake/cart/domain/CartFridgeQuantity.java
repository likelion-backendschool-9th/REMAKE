package com.remake.cart.domain;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;

/** 냉장고 이동 수량 보존. 냉장고 9개 단위는 그대로, 계량 단위만 ml로 환산한다. */
public record CartFridgeQuantity(BigDecimal amount, IngredientUnit unit) {
	public static CartFridgeQuantity from(BigDecimal amount, RecipeUnit unit) {
		CartItem.validateAmount(amount);
		if (unit == null || unit == RecipeUnit.PINCH || unit == RecipeUnit.TO_TASTE) {
			throw new IllegalArgumentException("양을 셀 수 없는 재료는 냉장고로 옮길 수 없습니다.");
		}
		BigDecimal factor = switch (unit) {
			case TBSP -> BigDecimal.valueOf(15);
			case TSP -> BigDecimal.valueOf(5);
			case CUP -> BigDecimal.valueOf(200);
			default -> BigDecimal.ONE;
		};
		BigDecimal converted = amount.multiply(factor);
		CartItem.validateAmount(converted);
		IngredientUnit fridgeUnit = switch (unit) {
			case TBSP, TSP, CUP -> IngredientUnit.ML;
			default -> IngredientUnit.valueOf(unit.name());
		};
		return new CartFridgeQuantity(converted, fridgeUnit);
	}
}
