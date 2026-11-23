package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 재료 직접 추가 요청. 값 검사는 여기(Bean Validation)와 도메인 두 곳에서 하고, 여기서 막으면 fieldErrors가 붙는다.
 * 단위는 문자열로 받아 냉장고 9개 코드가 아니면 INVALID_UNIT으로 답한다(enum으로 받으면 INVALID_INPUT이 된다).
 */
public record IngredientCreateRequest(
		@NotBlank @Size(max = 50) String name,
		@NotNull IngredientCategoryCode categoryCode,
		@NotNull StorageLocation storage,
		@NotNull LocalDate expirationDate,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 7, fraction = 3) BigDecimal amount,
		@NotBlank @Schema(description = "냉장고 단위 코드 9개 (G, KG, ML, L, EA, PACK, BUNCH, PIECE, SHEET)") String unit,
		@PositiveOrZero Long price) {

	public RegisterIngredientCommand toCommand(Long userId) {
		return new RegisterIngredientCommand(userId, name, categoryCode, storage, expirationDate, null, amount,
				parseUnit(unit), price, null);
	}

	/** 냉장고 단위 9개 코드만 허용한다. 레시피 단위(TBSP 등)나 모르는 값은 INVALID_UNIT. */
	static IngredientUnit parseUnit(String value) {
		try {
			return IngredientUnit.valueOf(value);
		}
		catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.INVALID_UNIT);
		}
	}
}
