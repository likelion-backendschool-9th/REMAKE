package com.remake.notification.domain;

/**
 * 알림 종류. DB·API는 이 enum 이름(EXPIRY_SOON 등)을 쓰고,
 * 이벤트 속성 notification_type은 {@link #eventValue()}(expiry_soon 등)를 쓴다.
 */
public enum NotificationType {

	EXPIRY_SOON,
	EXPIRED,
	MEAL_PLAN_COMPLETED,
	MEAL_PLAN_FAILED;

	/** 이벤트 정의서 E66 notification_type 값 */
	public String eventValue() {
		return switch (this) {
			case EXPIRY_SOON -> "expiry_soon";
			case EXPIRED -> "expired";
			case MEAL_PLAN_COMPLETED -> "meal_plan_completed";
			case MEAL_PLAN_FAILED -> "meal_plan_failed";
		};
	}
}
