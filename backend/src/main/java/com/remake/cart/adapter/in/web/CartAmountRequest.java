package com.remake.cart.adapter.in.web;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record CartAmountRequest(@NotNull @DecimalMin(value = "0", inclusive = false)
		@Digits(integer = 7, fraction = 3) BigDecimal amount) {
	@JsonAnySetter
	public void rejectExtraField(String name, Object value) {
		throw new IllegalArgumentException("수량 외의 입력은 받지 않습니다.");
	}
}
