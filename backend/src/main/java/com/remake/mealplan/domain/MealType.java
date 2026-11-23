package com.remake.mealplan.domain;

/** 끼니. DB와 JSON에는 이름 그대로(BREAKFAST) 들어간다. 선언 순서(아침·점심·저녁)가 저장·표시 순서다. */
public enum MealType {
	BREAKFAST,
	LUNCH,
	DINNER
}
