package com.remake.recipe.domain;

import java.math.BigDecimal;

/**
 * 레시피 재료 한 줄.
 *
 * @param ingredientId 생성할 때 쓴 내 냉장고 재료 id(요리 완료 차감 대상). 냉장고에 없는 재료(보충)면 null
 * @param amount       양. null이면 "약간"
 */
public record RecipeIngredient(Long ingredientId, String name, BigDecimal amount, RecipeUnit unit) {

	public static final int NAME_MAX_LENGTH = 100;
	/** numeric(10,3): 정수 7자리, 소수 3자리 */
	private static final int AMOUNT_MAX_INTEGER_DIGITS = 7;
	private static final int AMOUNT_MAX_SCALE = 3;

	public RecipeIngredient {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("레시피 재료 이름은 비워 둘 수 없습니다.");
		}
		name = name.strip();
		if (name.length() > NAME_MAX_LENGTH) {
			throw new IllegalArgumentException("레시피 재료 이름은 " + NAME_MAX_LENGTH + "자 이내여야 합니다.");
		}
		if (unit == null) {
			throw new IllegalArgumentException("레시피 재료 단위는 필수입니다.");
		}
		if (amount != null) {
			validateAmount(amount);
		}
	}

	private static void validateAmount(BigDecimal amount) {
		if (amount.signum() <= 0) {
			throw new IllegalArgumentException("레시피 재료 양은 0보다 커야 합니다.");
		}
		BigDecimal stripped = amount.stripTrailingZeros();
		if (stripped.scale() > AMOUNT_MAX_SCALE) {
			throw new IllegalArgumentException("레시피 재료 양은 소수 셋째 자리까지입니다.");
		}
		if (stripped.precision() - stripped.scale() > AMOUNT_MAX_INTEGER_DIGITS) {
			throw new IllegalArgumentException("레시피 재료 양이 너무 큽니다.");
		}
	}
}
