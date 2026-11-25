package com.remake.notification.domain;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** 알림 문구. 피그마·정책정의서 8장 문구를 한곳에 둔다. */
public final class NotificationMessages {

	private static final DateTimeFormatter MEAL_PLAN_START =
			DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN);

	private NotificationMessages() {
	}

	public static String expirySoon(String ingredientName) {
		Objects.requireNonNull(ingredientName, "ingredientName");
		return ingredientName + "의 소비기한이 얼마 남지 않았어요. 제품의 포장지에서 사용 기한을 확인해 주세요";
	}

	public static String expired(long count) {
		if (count < 1) {
			throw new IllegalArgumentException("count must be >= 1");
		}
		return "기한이 지난 재료 " + count + "개가 있어요. 냉장고를 확인해 주세요";
	}

	/**
	 * 제안 문구(정책 8장). 식단표 시작일과 실제 생성 일수.
	 *
	 * <p>재료가 부족해 최대 일수를 못 채웠으면 "N일치만 만들었어요"를, 그때 장바구니에 담은 재료가 있으면
	 * "부족한 재료 N개를 장바구니에 담았어요"를 덧붙인다(domain-rules 5장, 문구는 제안). 따로 토스트는 없다.
	 *
	 * @param maxDays        접수 때 요금제의 최대 일수(일반 5, 구독 7)
	 * @param cartAddedCount 부족 재료로 장바구니에 담은 행 수
	 */
	public static String mealPlanCompleted(LocalDate startDate, int generatedDays, int maxDays, int cartAddedCount) {
		Objects.requireNonNull(startDate, "startDate");
		if (generatedDays < 1) {
			throw new IllegalArgumentException("generatedDays must be >= 1");
		}
		StringBuilder message = new StringBuilder()
				.append(startDate.format(MEAL_PLAN_START)).append("부터 ")
				.append(generatedDays).append("일치 식단표가 완성됐어요");
		if (generatedDays < maxDays) {
			message.append(". 재료가 부족해 ").append(generatedDays).append("일치만 만들었어요");
			if (cartAddedCount > 0) {
				message.append(". 부족한 재료 ").append(cartAddedCount).append("개를 장바구니에 담았어요");
			}
		}
		return message.toString();
	}

	public static String mealPlanFailed(MealPlanFailReason reason) {
		return switch (Objects.requireNonNull(reason, "reason")) {
			case NO_INGREDIENTS -> "냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요";
			case GENERIC -> "식단표를 만들지 못했어요. 잠시 후 다시 만들어 주세요. 생성 횟수는 차감되지 않았어요";
		};
	}
}
