package com.remake.cart.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.CleanUpMealPlanSlotCartItemsUseCase;
import com.remake.cart.application.port.out.MealPlanSlotCartItemPort;
import com.remake.cart.application.port.out.MealPlanSlotCartItemPort.SlotCartRow;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class CleanUpMealPlanSlotCartItemsService implements CleanUpMealPlanSlotCartItemsUseCase {

	private final MealPlanSlotCartItemPort port;
	private final GetRecipeSaveStateUseCase saveStates;

	@Override
	@Transactional
	public int cleanUp(Long userId, Map<Long, Long> slotRecipeIds) {
		if (userId == null || slotRecipeIds == null) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		if (slotRecipeIds.isEmpty()) {
			return 0;
		}
		List<SlotCartRow> rows = port.lockBySlots(userId, slotRecipeIds.keySet()).stream()
				.filter(row -> Objects.equals(row.recipeId(), slotRecipeIds.get(row.mealPlanSlotId())))
				.toList();
		Map<Long, Boolean> saved = new HashMap<>();
		List<Long> removed = rows.stream()
				.filter(row -> row.untouched() && !saved.computeIfAbsent(row.recipeId(),
						recipeId -> saveStates.getSaveState(userId, recipeId, null).saved()))
				.map(SlotCartRow::id)
				.toList();
		List<Long> kept = rows.stream().map(SlotCartRow::id).filter(id -> !removed.contains(id)).toList();
		if (!removed.isEmpty()) {
			port.deleteAllById(removed);
		}
		if (!kept.isEmpty()) {
			port.unlinkSlot(kept);
		}
		return removed.size();
	}
}
