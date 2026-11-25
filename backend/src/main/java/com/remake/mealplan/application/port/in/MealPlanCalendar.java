package com.remake.mealplan.application.port.in;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 월 달력(API 39).
 *
 * @param hasAnyMealPlan 완성된 식단표가 하나라도 있는지(달 상관없이). false면 화면이 달력 대신 빈 화면(식단표 만들기)을 보인다
 * @param periods        그 달에 걸친 식단표 기간(시작일 순). 달 밖으로 이어지는 기간도 자르지 않고 그대로 준다
 * @param datesWithMeals 그 달에서 식단 칸이 있는 날짜(날짜 순, 중복 없음)
 */
public record MealPlanCalendar(YearMonth month, boolean hasAnyMealPlan, List<Period> periods, List<LocalDate> datesWithMeals) {

	public record Period(Long mealPlanId, LocalDate startDate, LocalDate endDate) {
	}
}
