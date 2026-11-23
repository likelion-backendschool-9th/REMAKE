package com.remake.mealplan.domain;

import java.math.BigDecimal;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;

/**
 * 단위 환산 표(domain-rules "Units"). 같은 종류끼리만 바꾼다: 무게는 g, 부피는 ml(큰술 15·작은술 5·컵 200)로 모은다.
 * 개수 단위(개·팩·단·쪽·장)는 서로 바꾸지 않아 같은 단위끼리만 같은 종류다. 꼬집·약간은 양이 없다.
 */
final class StockUnits {

	/** 기준 단위로 모은 양의 종류. 이 값이 같아야 서로 더하고 뺄 수 있다. */
	enum Dimension {
		WEIGHT, VOLUME, EA, PACK, BUNCH, PIECE, SHEET
	}

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
		return RecipeUnit.valueOf(unit.name());
	}

	private static Dimension dimensionOf(RecipeUnit unit) {
		return switch (unit) {
			case G, KG -> Dimension.WEIGHT;
			case ML, L, TBSP, TSP, CUP -> Dimension.VOLUME;
			case EA -> Dimension.EA;
			case PACK -> Dimension.PACK;
			case BUNCH -> Dimension.BUNCH;
			case PIECE -> Dimension.PIECE;
			case SHEET -> Dimension.SHEET;
			case PINCH, TO_TASTE -> null;
		};
	}

	private static BigDecimal factorOf(RecipeUnit unit) {
		return switch (unit) {
			case KG, L -> BigDecimal.valueOf(1000);
			case CUP -> BigDecimal.valueOf(200);
			case TBSP -> BigDecimal.valueOf(15);
			case TSP -> BigDecimal.valueOf(5);
			default -> BigDecimal.ONE;
		};
	}
}
