package com.remake.mealplan.application.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import com.remake.mealplan.domain.MealPlanSlot;

public interface LoadMealPlanSlotPort {

	/** 회원의 모든 식단표에서 from~to(둘 다 포함) 날짜의 칸. 날짜·끼니 순. */
	List<MealPlanSlot> loadUserSlotsBetween(Long userId, LocalDate from, LocalDate to);

	/** 회원의 모든 식단표에서 from(포함) 이후 날짜의 칸. 날짜·끼니 순. */
	List<MealPlanSlot> loadUserSlotsFrom(Long userId, LocalDate from);

	/** 칸들을 잠그고 읽는다(id 순). 이미 지워진 칸은 빠진다. 쓰기 트랜잭션 안에서만 쓴다. */
	List<MealPlanSlot> loadByIdsForUpdate(Collection<Long> slotIds);
}
