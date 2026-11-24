package com.remake.recipe.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientUnit;

/**
 * 단위 환산 표(domain-rules "Units", #63). 단위 환산은 이 클래스 한 곳에서만 한다(요리 차감, 식단표 재료 배정, 장바구니 → 냉장고).
 * <ul>
 * <li>같은 종류끼리만 바꾼다. 무게는 g, 부피는 ml로 모은다. 큰술 15ml·작은술 5ml·컵 200ml는 정의서의 제안 값이라 상수로 둔다.</li>
 * <li>개수 단위(개·팩·단·쪽·장)는 서로 바꾸지 않는다. 같은 단위끼리만 같은 종류다.</li>
 * <li>꼬집·약간은 양이 없어 어느 종류에도 속하지 않는다(자동 차감 안 함).</li>
 * </ul>
 */
public final class UnitConversion {

	public static final BigDecimal TBSP_ML = BigDecimal.valueOf(15);
	public static final BigDecimal TSP_ML = BigDecimal.valueOf(5);
	public static final BigDecimal CUP_ML = BigDecimal.valueOf(200);

	/** 냉장고 양과 같은 자릿수(소수 셋째 자리까지). */
	private static final int AMOUNT_SCALE = 3;
	private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

	/** 기준 단위(g, ml, 개수 단위는 그 단위)로 모았을 때의 종류. 이 값이 같아야 서로 바꿀 수 있다. */
	public enum Dimension {
		WEIGHT, VOLUME, EA, PACK, BUNCH, PIECE, SHEET
	}

	private UnitConversion() {
	}

	/** 꼬집·약간은 비어 있다. */
	public static Optional<Dimension> dimensionOf(RecipeUnit unit) {
		return Optional.ofNullable(switch (unit) {
			case G, KG -> Dimension.WEIGHT;
			case ML, L, TBSP, TSP, CUP -> Dimension.VOLUME;
			case EA -> Dimension.EA;
			case PACK -> Dimension.PACK;
			case BUNCH -> Dimension.BUNCH;
			case PIECE -> Dimension.PIECE;
			case SHEET -> Dimension.SHEET;
			case PINCH, TO_TASTE -> null;
		});
	}

	/** 기준 단위 한 개가 몇 g·ml인지(개수 단위는 1). 꼬집·약간은 1이지만 dimensionOf가 비어 있어 쓰지 않는다. */
	public static BigDecimal factorOf(RecipeUnit unit) {
		return switch (unit) {
			case KG, L -> THOUSAND;
			case CUP -> CUP_ML;
			case TBSP -> TBSP_ML;
			case TSP -> TSP_ML;
			default -> BigDecimal.ONE;
		};
	}

	/** 큰술·작은술·컵(냉장고에 없는 계량 단위)인지. */
	public static boolean isMeasure(RecipeUnit unit) {
		return unit == RecipeUnit.TBSP || unit == RecipeUnit.TSP || unit == RecipeUnit.CUP;
	}

	/** 냉장고 단위 9개는 레시피 단위 14개 안에 이름 그대로 있다. */
	public static RecipeUnit asRecipeUnit(IngredientUnit unit) {
		return RecipeUnit.valueOf(unit.name());
	}

	/**
	 * 레시피 양을 냉장고 단위로 바꾼다. 종류가 다르거나(양파 1개 ↔ 300g, 팩 ↔ g) 꼬집·약간이면 비어 있다.
	 * 결과는 소수 셋째 자리에서 반올림한다.
	 */
	public static Optional<BigDecimal> convert(BigDecimal amount, RecipeUnit from, IngredientUnit to) {
		if (amount == null) {
			return Optional.empty();
		}
		RecipeUnit target = asRecipeUnit(to);
		Optional<Dimension> fromDimension = dimensionOf(from);
		if (fromDimension.isEmpty() || !fromDimension.equals(dimensionOf(target))) {
			return Optional.empty();
		}
		BigDecimal base = amount.multiply(factorOf(from));
		return Optional.of(base.divide(factorOf(target), AMOUNT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros());
	}
}
