package com.remake.receipt.domain;

import java.math.BigDecimal;
import java.util.Locale;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientUnit;

public record OcrQuantity(BigDecimal amount, IngredientUnit unit) {

	public static OcrQuantity normalize(BigDecimal amount, String rawUnit) {
		// 수량을 읽지 못하면 단위도 함께 기본값으로 돌린다.
		if (amount == null || amount.signum() <= 0) {
			return new OcrQuantity(BigDecimal.ONE, IngredientUnit.EA);
		}

		String unit = rawUnit == null ? "" : rawUnit.strip().toUpperCase(Locale.ROOT);
		IngredientUnit normalizedUnit = switch (unit) {
			case "", "EA", "개", "마리", "알", "모", "포기" -> IngredientUnit.EA;
			case "PACK", "팩", "봉", "봉지", "캔", "병", "통" -> IngredientUnit.PACK;
			case "BUNCH", "단" -> IngredientUnit.BUNCH;
			case "PIECE", "쪽" -> IngredientUnit.PIECE;
			case "SHEET", "장" -> IngredientUnit.SHEET;
			case "G", "그램" -> IngredientUnit.G;
			case "KG", "킬로그램" -> IngredientUnit.KG;
			case "ML", "밀리리터" -> IngredientUnit.ML;
			case "L", "리터" -> IngredientUnit.L;
			default -> throw new BusinessException(ErrorCode.INVALID_INPUT);
		};
		return new OcrQuantity(amount, normalizedUnit);
	}
}
