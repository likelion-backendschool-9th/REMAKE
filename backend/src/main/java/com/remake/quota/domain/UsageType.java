package com.remake.quota.domain;

/** 생성 횟수 종류. usage_logs.type에 이름 그대로 들어간다. */
public enum UsageType {
	/** 레시피 생성(빠른·커스텀·다시 받기·식단 칸 교체). 하루 단위 */
	RECIPE,
	/** 식단표 생성. 한 달 단위 */
	MEAL_PLAN
}
