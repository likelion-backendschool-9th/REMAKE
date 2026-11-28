package com.remake.subscription.domain;

/**
 * 구독 상태. ACTIVE 이용 중 / CANCELED 해지 예약(다음 결제일까지 혜택) / ENDED 종료.
 */
public enum SubscriptionStatus {
	ACTIVE,
	CANCELED,
	ENDED;

	public boolean isUnfinished() {
		return this == ACTIVE || this == CANCELED;
	}
}
