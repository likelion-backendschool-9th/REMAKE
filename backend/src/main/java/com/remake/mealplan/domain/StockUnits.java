package com.remake.mealplan.domain;

import java.math.BigDecimal;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.recipe.domain.UnitConversion;
import com.remake.recipe.domain.UnitConversion.Dimension;

/**
 * 식단표 재료 배정용 단위 환산. 환산 표(종류·배수)는 레시피 영역의 UnitConversion 한 곳에 있고(#63), 여기서는 기준 단위 양으로 모으고 되돌리기만 한다.
 */
final class StockUnits {

	/** 기준 단위(g, ml, 개수 단위는 그 단위)로 바꾼 양. */
	record BaseAmount(Dimension dimension, BigDecimal amount) {
	}

	private StockUnits() {
	}

	/** 레시피 양을 기준 단위로 바꾼다. 꼬집·약간은 비어 있다. */
	static Optional<BaseAmount> toBase(BigDecimal amount, RecipeUnit unit) {
		Dimension dimension = dimensionOf(unit);
		if (dimension == null) {
			return Optional.empty();
		}
		return Optional.of(new BaseAmount(dimension, amount.multiply(factorOf(unit))));
	}

	/** 냉장고 양을 기준 단위로 바꾼다. 냉장고 단위 9개는 레시피 단위 14개 안에 이름 그대로 있다. */
	static BaseAmount toBase(BigDecimal amount, IngredientUnit unit) {
		return toBase(amount, asRecipeUnit(unit)).orElseThrow();
	}

	/** 기준 단위 양을 냉장고 단위로 되돌린다. */
	static BigDecimal fromBase(BigDecimal baseAmount, IngredientUnit unit) {
		return baseAmount.divide(factorOf(asRecipeUnit(unit))).stripTrailingZeros();
	}

	private static RecipeUnit asRecipeUnit(IngredientUnit unit) {
		return UnitConversion.asRecipeUnit(unit);
	}

	private static Dimension dimensionOf(RecipeUnit unit) {
		return UnitConversion.dimensionOf(unit).orElse(null);
	}

	private static BigDecimal factorOf(RecipeUnit unit) {
		return UnitConversion.factorOf(unit);
	}
}
