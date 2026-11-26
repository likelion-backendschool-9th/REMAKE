package com.remake.user.application.port.in;

import java.time.LocalDate;

import com.remake.user.domain.SubscriptionStatus;

/** API 57: 내 구독 상태·결제일 조회. */
public interface GetSubscriptionUseCase {

	SubscriptionView get(Long userId);

	record SubscriptionView(
			SubscriptionViewStatus status,
			long price,
			LocalDate nextBillingDate,
			LocalDate currentPeriodEnd) {
	}

	/**
	 * API 응답용 상태. 미구독은 NONE(DB에는 ENDED만 있거나 행 없음).
	 * ACTIVE·CANCELED는 도메인과 같고, ENDED는 NONE으로 내린다.
	 */
	enum SubscriptionViewStatus {
		NONE,
		ACTIVE,
		CANCELED;

		public static SubscriptionViewStatus from(SubscriptionStatus status) {
			if (status == null || status == SubscriptionStatus.ENDED) {
				return NONE;
			}
			return status == SubscriptionStatus.CANCELED ? CANCELED : ACTIVE;
		}
	}
}
