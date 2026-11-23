package com.remake.mealplan.domain;

/** 식단표 상태. 생성 요청을 받으면 GENERATING, 서버 작업이 끝나면 COMPLETED 또는 FAILED가 된다. */
public enum MealPlanStatus {
	GENERATING,
	COMPLETED,
	FAILED
}
