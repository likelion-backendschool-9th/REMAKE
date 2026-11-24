package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 재료 직접 추가 요청. 값 검사는 여기(Bean Validation)와 도메인 두 곳에서 하고, 여기서 막으면 fieldErrors가 붙는다.
 * 단위는 받지 않는다. 서버가 재료명과 카테고리로 정하고, 요청에 unit이 들어와도 읽지 않는다.
 */
public record IngredientCreateRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull IngredientCategoryCode categoryCode,
		@NotNull StorageLocation storage,
		@NotNull LocalDate expirationDate,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3) BigDecimal amount,
		@PositiveOrZero Long price) {

	public RegisterIngredientCommand toCommand(Long userId) {
		return new RegisterIngredientCommand(userId, name, categoryCode, storage, expirationDate, null, amount,
				null, price, null);
	}
}
