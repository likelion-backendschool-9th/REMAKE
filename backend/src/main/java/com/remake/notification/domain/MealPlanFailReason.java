package com.remake.notification.domain;

/** 식단표 생성 실패 알림 문구 구분. 05 생성 작업이 넘긴다. */
public enum MealPlanFailReason {
	/** 재료 부족 */
	NO_INGREDIENTS,
	/** AI 오류·시간 초과 등 그 외 */
	GENERIC
}
