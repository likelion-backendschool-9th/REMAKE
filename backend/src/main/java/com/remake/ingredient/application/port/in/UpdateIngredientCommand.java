package com.remake.ingredient.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

/**
 * 재료 수정 요청. null인 값은 바꾸지 않는다. 수량은 사용·폐기한 적이 없을 때만 고칠 수 있고(남은 양과 처음 양을 함께 바꿈), 단위는 받지 않는다. 이름이나 카테고리가 바뀌면 서버가 단위를 다시 정한다(수량 숫자는 그대로).
 * 가격은 "바꾸지 않음"과 "가격 지움"을 나눠야 해서 PriceChange로 감싼다: price가 null이면 그대로, PriceChange(null)이면 지움.
 */
public record UpdateIngredientCommand(
		Long userId,
		Long ingredientId,
		String name,
		IngredientCategoryCode categoryCode,
		LocalDate expirationDate,
		StorageLocation storage,
		PriceChange price,
		BigDecimal amount) {

	public UpdateIngredientCommand(Long userId, Long ingredientId, String name, IngredientCategoryCode categoryCode,
			LocalDate expirationDate, StorageLocation storage, PriceChange price) {
		this(userId, ingredientId, name, categoryCode, expirationDate, storage, price, null);
	}

	public record PriceChange(Long value) {
	}
}
