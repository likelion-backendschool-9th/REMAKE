package com.remake.mealplan.adapter.in.web;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.remake.mealplan.application.port.in.MealPlanCalendar;

import io.swagger.v3.oas.annotations.media.Schema;

/** 월 달력 응답(API 39). */
public record MealPlanCalendarResponse(
		@Schema(type = "string", example = "2026-11") YearMonth month,
		@Schema(description = "완성된 식단표가 하나라도 있는지(달 상관없이). false면 달력 대신 빈 화면") boolean hasAnyMealPlan,
		@Schema(description = "그 달에 걸친 식단표 기간(시작일 순). 달 밖으로 이어지는 기간도 그대로") List<Period> periods,
		@Schema(description = "그 달에서 식단이 있는 날짜(날짜 순)") List<LocalDate> datesWithMeals) {

	public record Period(Long mealPlanId, LocalDate startDate, LocalDate endDate) {
	}

	static MealPlanCalendarResponse from(MealPlanCalendar calendar) {
		return new MealPlanCalendarResponse(calendar.month(), calendar.hasAnyMealPlan(),
				calendar.periods().stream()
						.map(period -> new Period(period.mealPlanId(), period.startDate(), period.endDate()))
						.toList(),
				calendar.datesWithMeals());
	}
}
