package com.remake.cart.application.port.in;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientUnit;

/** 영수증 쪽에서 품목별 검증을 마친 선택 요청. 이름·단위는 최종 냉장고 등록에 쓰는 서버 확정 값이다. */
public record CartDeductionCommand(
		Long cartItemId,
		String ingredientName,
		BigDecimal amount,
		IngredientUnit unit) {
}
