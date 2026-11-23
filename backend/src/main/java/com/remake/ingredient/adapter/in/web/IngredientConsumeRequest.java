package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;

import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.IngredientLogType;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** 사용했어요/버렸어요 요청. type은 USED 또는 DISCARDED, amount는 뺄 양(0보다 크고 재료의 단위 기준)이다. */
public record IngredientConsumeRequest(
		@NotNull IngredientLogType type,
		@NotNull @Digits(integer = 7, fraction = 3) BigDecimal amount) {

	public ConsumeIngredientCommand toCommand(Long userId, Long ingredientId) {
		return new ConsumeIngredientCommand(userId, ingredientId, type, amount, ConsumeVia.CONSUME);
	}
}
