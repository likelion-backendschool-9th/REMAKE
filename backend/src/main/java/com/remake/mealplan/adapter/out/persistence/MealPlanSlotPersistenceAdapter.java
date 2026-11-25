package com.remake.mealplan.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.mealplan.application.port.out.MealPlanSlotPort;
import com.remake.mealplan.domain.MealPlanSlot;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class MealPlanSlotPersistenceAdapter implements MealPlanSlotPort {

	private final MealPlanSlotJpaRepository repository;

	@Override
	public Optional<MealPlanSlot> findByIdAndUserId(Long slotId, Long userId) {
		return repository.findByIdAndUserId(slotId, userId).map(MealPlanSlotJpaEntity::toDomain);
	}

	@Override
	public Optional<MealPlanSlot> findByIdAndUserIdForUpdate(Long slotId, Long userId) {
		return repository.findByIdAndUserIdForUpdate(slotId, userId).map(MealPlanSlotJpaEntity::toDomain);
	}

	@Override
	public void update(MealPlanSlot slot) {
		// 같은 트랜잭션에서 읽은 엔티티라 값을 옮기면 변경 감지로 저장된다
		repository.findById(slot.id()).orElseThrow().apply(slot);
	}
}
