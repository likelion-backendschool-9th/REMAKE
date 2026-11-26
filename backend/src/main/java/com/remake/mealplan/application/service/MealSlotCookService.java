package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.MarkMealSlotCookedUseCase;
import com.remake.mealplan.application.port.out.MealPlanSlotPort;
import com.remake.mealplan.domain.MealPlanSlot;

import lombok.RequiredArgsConstructor;

/** 오늘 식단 칸 요리 완료 표시(#77). 요리 차감(레시피 영역)이 부른다. */
@Service
@RequiredArgsConstructor
class MealSlotCookService implements MarkMealSlotCookedUseCase {

	private final MealPlanSlotPort mealPlanSlotPort;
	private final Clock clock;

	@Override
	@Transactional
	public void markCooked(Long userId, Long mealPlanSlotId, Long recipeId) {
		// 잠그고 읽는다: 식단표 덮어쓰기 저장(MealPlanResultWriter.complete)이 이 칸을 지우는 중이면 끝날 때까지 기다린다.
		// 레시피를 연 뒤 칸이 지워졌거나(칸 삭제·덮어쓰기) 다른 레시피로 바뀌었으면(칸 교체) 표시만 건너뛴다.
		// 사용자는 실제로 요리했으니 요리 완료(재료 차감)는 그대로 성공시킨다
		Optional<MealPlanSlot> found = mealPlanSlotPort.findByIdAndUserIdForUpdate(mealPlanSlotId, userId)
				.filter(s -> s.recipeId().equals(recipeId));
		if (found.isEmpty()) {
			return;
		}
		MealPlanSlot slot = found.get();
		LocalDateTime now = LocalDateTime.now(clock);
		// 다른 날 칸에서 연 레시피로 요리해도 그 칸은 표시하지 않는다(domain-rules: 오늘 칸만). 이미 요리한 칸은 처음 시각을 둔다
		if (!slot.slotDate().equals(LocalDate.from(now)) || slot.isCooked()) {
			return;
		}
		slot.markCooked(now);
		mealPlanSlotPort.update(slot);
	}
}
