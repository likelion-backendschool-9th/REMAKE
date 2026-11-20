package com.remake.ingredient.application.port.in;

import java.time.LocalDate;

import com.remake.ingredient.domain.StorageLocation;

/**
 * 재료 수정 요청. null인 값은 바꾸지 않는다.
 * 가격은 "바꾸지 않음"과 "가격 지움"을 나눠야 해서 PriceChange로 감싼다: price가 null이면 그대로, PriceChange(null)이면 지움.
 */
public record UpdateIngredientCommand(
		Long userId,
		Long ingredientId,
		LocalDate expirationDate,
		StorageLocation storage,
		PriceChange price) {

	public record PriceChange(Long value) {
	}
}
