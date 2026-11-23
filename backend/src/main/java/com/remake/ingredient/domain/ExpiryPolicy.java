package com.remake.ingredient.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 소비기한 판정 기준. 임박 기준 3일은 여기 한 곳에만 둔다. 알림용 기준과 냉장고 화면의
 * 위험·임박·여유 판정(dDay, statusOf)이 SOON_DAYS를 같이 쓴다.
 * 알림 쪽은 이 숫자를 모르고, UseCase가 돌려주는 결과만 받는다.
 * <ul>
 *   <li>임박: 오늘부터 3일 뒤까지(오늘이 기한인 D-DAY 포함). 알림의 EXPIRY_SOON</li>
 *   <li>만료: 소비기한이 오늘보다 이전. 알림의 EXPIRED</li>
 *   <li>어제 만료: 소비기한이 어제. 만료 이벤트(ingredient_expired)를 재료당 한 번만 보내려고 쓴다</li>
 * </ul>
 * 알림 기준은 D-DAY를 임박으로 보지만 냉장고 화면의 위험·임박 표시는 D-DAY를 위험으로 본다(의도한 차이).
 * 현재 시각은 읽지 않고, 오늘 날짜를 인수로 받는다.
 */
public final class ExpiryPolicy {

	/** 임박 기준 일수(오늘 포함 오늘부터 3일 뒤까지) */
	public static final int SOON_DAYS = 3;

	private ExpiryPolicy() {
	}

	public static LocalDate expiringFrom(LocalDate today) {
		return today;
	}

	public static LocalDate expiringTo(LocalDate today) {
		return today.plusDays(SOON_DAYS);
	}

	/** 이 날짜보다 소비기한이 이전이면 만료 */
	public static LocalDate expiredBefore(LocalDate today) {
		return today;
	}

	public static LocalDate expiredYesterday(LocalDate today) {
		return today.minusDays(1);
	}

	/** 오늘부터 소비기한까지 남은 일수. 오늘이 기한이면 0, 지났으면 음수 */
	public static long dDay(LocalDate expirationDate, LocalDate today) {
		return ChronoUnit.DAYS.between(today, expirationDate);
	}

	/** 냉장고 화면 상태: 지났거나 D-DAY면 DANGER, D-1~D-3이면 SOON, 그 외 OK */
	public static ExpiryStatus statusOf(long dDay) {
		if (dDay <= 0) {
			return ExpiryStatus.DANGER;
		}
		return dDay <= SOON_DAYS ? ExpiryStatus.SOON : ExpiryStatus.OK;
	}

	/**
	 * 소비기한을 모를 때 카테고리 기본 보관 기간으로 추정한다. 기준일은 영수증이면 구매일, 직접 추가면 등록한 날이다.
	 * 추정한 날짜는 Ingredient에 추정 표시(expirationEstimated)와 함께 저장한다.
	 */
	public static LocalDate estimateExpirationDate(LocalDate baseDate, int defaultShelfLifeDays) {
		return baseDate.plusDays(defaultShelfLifeDays);
	}
}
