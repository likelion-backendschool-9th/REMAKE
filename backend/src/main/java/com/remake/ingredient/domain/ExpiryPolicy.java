package com.remake.ingredient.domain;

import java.time.LocalDate;

/**
 * 소비기한 판정 기준. 임박 기준 3일은 여기 한 곳에만 둔다. 지금은 알림용 기준만 있고,
 * 냉장고 화면의 위험·임박·여유 판정을 만들 때 이 클래스에 함께 두어 SOON_DAYS를 같이 쓴다.
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
}
