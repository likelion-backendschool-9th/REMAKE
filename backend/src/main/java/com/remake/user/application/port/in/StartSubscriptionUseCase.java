package com.remake.user.application.port.in;

/** API 59: 구독 첫 결제·시작. */
public interface StartSubscriptionUseCase {

	GetSubscriptionUseCase.SubscriptionView start(Command command);

	record Command(Long userId, String authKey, String customerKey) {
	}
}
