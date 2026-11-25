package com.remake.mealplan.application.port.in;

import java.time.YearMonth;

/** 월 달력 조회(API 39). 지난 달·앞으로의 달 모두 볼 수 있다. */
public interface GetMealPlanCalendarUseCase {

	/** 회원 본인의 식단표만. 식단이 없으면 periods·datesWithMeals가 빈 목록이다. */
	MealPlanCalendar getCalendar(Long userId, YearMonth month);
}
