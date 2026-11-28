package com.remake.cart.application.port.out;

import java.util.Collection;
import java.util.List;

public interface MealPlanSlotCartItemPort {

	/** 그 칸들에서 담은 내 행을 id 순서로 잠그고 읽는다. */
	List<SlotCartRow> lockBySlots(Long userId, Collection<Long> mealPlanSlotIds);

	void deleteAllById(Collection<Long> cartItemIds);

	/** 칸 연결만 비운다. updated_at은 그대로 둔다(사용자의 수량 수정이 아니다). */
	void unlinkSlot(Collection<Long> cartItemIds);

	/** @param untouched 담은 뒤 수량을 고치거나 차감한 적이 없는지(updated_at = created_at) */
	record SlotCartRow(Long id, Long recipeId, Long mealPlanSlotId, boolean untouched) { }
}
