package com.remake.user.application.port.in;

/** API 58: 토스 카드 등록 창 초기값. */
public interface PrepareBillingAuthUseCase {

	BillingAuthView prepare(Long userId);

	record BillingAuthView(String customerKey, String orderName, long amount) {
	}
}
