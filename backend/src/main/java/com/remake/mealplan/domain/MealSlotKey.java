package com.remake.mealplan.domain;

import java.time.LocalDate;

/** 식단 칸 자리(날짜·끼니). */
public record MealSlotKey(LocalDate date, MealType mealType) {

	public MealSlotKey {
		if (date == null || mealType == null) {
			throw new IllegalArgumentException("칸 자리에는 날짜와 끼니가 필요합니다.");
		}
	}
}
