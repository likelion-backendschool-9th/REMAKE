package com.remake.mealplan.application.port.in;

import java.time.LocalDate;

/** 날짜별 끼니 조회(API 40). 지난 날짜도 볼 수 있고, 레시피를 새로 만들지 않고 저장된 칸만 읽는다. */
public interface GetMealPlanDayUseCase {

	/** 회원 본인의 칸만. 그날 칸이 없으면 slots가 빈 목록이다. */
	MealPlanDay getDay(Long userId, LocalDate date);
}
