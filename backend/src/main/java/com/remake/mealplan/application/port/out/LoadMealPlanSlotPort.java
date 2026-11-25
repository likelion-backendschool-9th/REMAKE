package com.remake.mealplan.application.port.out;

import java.time.LocalDate;
import java.util.List;

import com.remake.mealplan.domain.MealPlanSlot;

public interface LoadMealPlanSlotPort {

	/** 회원의 모든 식단표에서 from~to(둘 다 포함) 날짜의 칸. 날짜·끼니 순. */
	List<MealPlanSlot> loadUserSlotsBetween(Long userId, LocalDate from, LocalDate to);

	/** 회원의 모든 식단표에서 from(포함) 이후 날짜의 칸. 날짜·끼니 순. */
	List<MealPlanSlot> loadUserSlotsFrom(Long userId, LocalDate from);
}
