package com.remake.mealplan.application.port.out;

import java.util.Collection;
import java.util.List;

import com.remake.mealplan.domain.MealPlanSlot;

public interface SaveMealPlanSlotPort {

	/** 칸들을 저장하고, 넘긴 순서대로 id가 채워진 칸을 돌려준다. */
	List<MealPlanSlot> saveAll(List<MealPlanSlot> slots);

	void deleteAllById(Collection<Long> slotIds);
}
