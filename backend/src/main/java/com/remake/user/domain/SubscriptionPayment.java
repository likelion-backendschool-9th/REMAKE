package com.remake.user.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 구독 첫 결제·자동결제 시도 한 건. */
public record SubscriptionPayment(
		Long id,
		Long subscriptionId,
		Long userId,
		String orderId,
		String paymentKey,
		long amount,
		SubscriptionPaymentStatus status,
		String failureCode,
		LocalDate periodStart,
		LocalDate periodEnd,
		LocalDateTime approvedAt,
		LocalDateTime createdAt) {

	public static SubscriptionPayment done(Long subscriptionId, Long userId, String orderId, String paymentKey,
			long amount, LocalDate periodStart, LocalDate periodEnd, LocalDateTime approvedAt) {
		return new SubscriptionPayment(null, subscriptionId, userId, orderId, paymentKey, amount,
				SubscriptionPaymentStatus.DONE, null, periodStart, periodEnd, approvedAt, approvedAt);
	}

	public static SubscriptionPayment restore(Long id, Long subscriptionId, Long userId, String orderId,
			String paymentKey, long amount, SubscriptionPaymentStatus status, String failureCode,
			LocalDate periodStart, LocalDate periodEnd, LocalDateTime approvedAt, LocalDateTime createdAt) {
		return new SubscriptionPayment(id, subscriptionId, userId, orderId, paymentKey, amount, status, failureCode,
				periodStart, periodEnd, approvedAt, createdAt);
	}

	public SubscriptionPayment withId(Long id) {
		return new SubscriptionPayment(id, subscriptionId, userId, orderId, paymentKey, amount, status, failureCode,
				periodStart, periodEnd, approvedAt, createdAt);
	}
}
