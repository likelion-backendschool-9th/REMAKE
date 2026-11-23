package com.remake.ingredient.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

/**
 * 재료 등록 요청. 직접 추가 API와 영수증 확정이 함께 쓴다.
 *
 * @param expirationDate      소비기한. null이면 재료 쪽이 카테고리 기본 보관 일수로 추정한다
 * @param expirationBaseDate  소비기한을 추정할 때의 기준일. 영수증은 구매일, null이면 오늘(등록일)
 * @param receiptItemId       영수증에서 등록한 것이면 receipt_items의 id, 직접 추가면 null
 */
public record RegisterIngredientCommand(
		Long userId,
		String name,
		IngredientCategoryCode categoryCode,
		StorageLocation storage,
		LocalDate expirationDate,
		LocalDate expirationBaseDate,
		BigDecimal amount,
		IngredientUnit unit,
		Long price,
		Long receiptItemId) {
}
