package com.remake.mealplan.application.port.out;

import java.util.Optional;

import com.remake.mealplan.domain.MealPlanSlot;

/** 식단 칸 읽기·저장. */
public interface MealPlanSlotPort {

	/** userId의 식단표에 속한 칸. 없거나 다른 사용자의 칸이면 비어 있다. */
	Optional<MealPlanSlot> findByIdAndUserId(Long slotId, Long userId);

	/** findByIdAndUserId와 같지만 행을 잠그고 읽는다. 쓰기 트랜잭션 안에서만 쓴다. 잠금은 트랜잭션이 끝날 때 풀린다. */
	Optional<MealPlanSlot> findByIdAndUserIdForUpdate(Long slotId, Long userId);

	/** 이미 저장된 칸의 바뀔 수 있는 값(레시피, 요리 시각)을 저장한다. */
	void update(MealPlanSlot slot);
}
