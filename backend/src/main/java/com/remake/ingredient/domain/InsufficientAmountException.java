package com.remake.ingredient.domain;

import java.math.BigDecimal;

/** 남은 양보다 많이 빼려 했다. Service가 INSUFFICIENT_QUANTITY로 바꾸고 남은 양을 응답에 담는다. */
public class InsufficientAmountException extends IllegalArgumentException {

	private final BigDecimal remaining;

	public InsufficientAmountException(BigDecimal remaining) {
		super("남은 양보다 많이 뺄 수 없습니다.");
		this.remaining = remaining;
	}

	public BigDecimal remaining() {
		return remaining;
	}
}
