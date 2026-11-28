package com.remake.subscription.application.port.in;

/** API 60: 구독 해지 예약. */
public interface CancelSubscriptionUseCase {

	GetSubscriptionUseCase.SubscriptionView cancel(Long userId);
}
