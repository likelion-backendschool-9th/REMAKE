package com.remake.subscription.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 프리미엄 구독(토스 카드 자동결제). 미종료(ACTIVE|CANCELED)는 회원당 하나.
 */
public record Subscription(
		Long id,
		Long userId,
		SubscriptionStatus status,
		String customerKey,
		String billingKeyEncrypted,
		long price,
		LocalDate currentPeriodStart,
		LocalDate nextBillingDate,
		LocalDateTime canceledAt,
		LocalDateTime endedAt,
		LocalDateTime createdAt) {

	public static final long MONTHLY_PRICE_KRW = 9900L;
	public static final String ORDER_NAME = "REMAKE 프리미엄 구독";

	public static Subscription start(Long userId, String customerKey, String billingKeyEncrypted,
			LocalDate periodStart, LocalDateTime createdAt) {
		if (userId == null) {
			throw new IllegalArgumentException("userId가 필요합니다.");
		}
		if (customerKey == null || customerKey.isBlank()) {
			throw new IllegalArgumentException("customerKey가 필요합니다.");
		}
		if (billingKeyEncrypted == null || billingKeyEncrypted.isBlank()) {
			throw new IllegalArgumentException("billingKey가 필요합니다.");
		}
		if (periodStart == null || createdAt == null) {
			throw new IllegalArgumentException("기간·생성 시각이 필요합니다.");
		}
		return new Subscription(null, userId, SubscriptionStatus.ACTIVE, customerKey, billingKeyEncrypted,
				MONTHLY_PRICE_KRW, periodStart, nextBillingDate(periodStart), null, null, createdAt);
	}

	public static Subscription restore(Long id, Long userId, SubscriptionStatus status, String customerKey,
			String billingKeyEncrypted, long price, LocalDate currentPeriodStart, LocalDate nextBillingDate,
			LocalDateTime canceledAt, LocalDateTime endedAt, LocalDateTime createdAt) {
		return new Subscription(id, userId, status, customerKey, billingKeyEncrypted, price, currentPeriodStart,
				nextBillingDate, canceledAt, endedAt, createdAt);
	}

	/** 시작일 + 1개월. 그 달에 같은 일이 없으면 말일. */
	public static LocalDate nextBillingDate(LocalDate start) {
		YearMonth target = YearMonth.from(start).plusMonths(1);
		int day = Math.min(start.getDayOfMonth(), target.lengthOfMonth());
		return target.atDay(day);
	}

	public boolean isUnfinished() {
		return status.isUnfinished();
	}

	public Subscription scheduleCancel(LocalDateTime canceledAt) {
		if (!isUnfinished()) {
			throw new IllegalStateException("종료된 구독은 해지할 수 없습니다.");
		}
		if (status == SubscriptionStatus.CANCELED) {
			return this;
		}
		if (canceledAt == null) {
			throw new IllegalArgumentException("해지 시각이 필요합니다.");
		}
		return new Subscription(id, userId, SubscriptionStatus.CANCELED, customerKey, billingKeyEncrypted, price,
				currentPeriodStart, nextBillingDate, canceledAt, endedAt, createdAt);
	}

	public Subscription end(LocalDateTime endedAt) {
		if (status == SubscriptionStatus.ENDED) {
			return this;
		}
		if (endedAt == null) {
			throw new IllegalArgumentException("종료 시각이 필요합니다.");
		}
		return new Subscription(id, userId, SubscriptionStatus.ENDED, customerKey, null, price, currentPeriodStart,
				nextBillingDate, canceledAt, endedAt, createdAt);
	}

	public Subscription withId(Long id) {
		return new Subscription(id, userId, status, customerKey, billingKeyEncrypted, price, currentPeriodStart,
				nextBillingDate, canceledAt, endedAt, createdAt);
	}

	public long subscribedDays(LocalDateTime at) {
		if (createdAt == null || at == null) {
			return 0L;
		}
		long days = java.time.temporal.ChronoUnit.DAYS.between(createdAt.toLocalDate(), at.toLocalDate());
		return Math.max(0L, days);
	}
}
