package com.remake.subscription.application.port.in;

/**
 * 7일 안 전액 환불(#343). 혜택 미사용일 때만 토스 취소·구독 종료·E89.
 */
public interface RefundSubscriptionUseCase {

	GetSubscriptionUseCase.SubscriptionView refund(Long userId);
}
