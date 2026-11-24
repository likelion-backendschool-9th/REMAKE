package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.ingredient.application.port.in.ConsumeIngredientResult;

/**
 * 사용·폐기 응답(API 18). 남은 양이 0이라 재료를 지웠으면 deleted가 true이다.
 * costAmount는 버린 재료의 가격 비례 금액이고, 사용했거나 가격이 없으면 필드가 빠진다.
 */
public record IngredientConsumeResponse(
		BigDecimal remainingAmount,
		boolean deleted,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long costAmount) {

	public static IngredientConsumeResponse from(ConsumeIngredientResult result) {
		return new IngredientConsumeResponse(result.remainingAmount(), result.deleted(), result.costAmount());
	}
}
