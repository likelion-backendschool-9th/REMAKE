package com.remake.mealplan.application.port.out;

import java.util.Collection;
import java.util.List;

import com.remake.mealplan.domain.MealPlanSlot;

public interface SaveMealPlanSlotPort {

	void saveAll(List<MealPlanSlot> slots);

	void deleteAllById(Collection<Long> slotIds);
}
