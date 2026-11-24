package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
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
		MealPlanSlot slot = mealPlanSlotPort.findByIdAndUserId(mealPlanSlotId, userId)
				.filter(s -> s.recipeId().equals(recipeId))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "식단 칸을 확인해 주세요."));
		LocalDateTime now = LocalDateTime.now(clock);
		// 다른 날 칸에서 연 레시피로 요리해도 그 칸은 표시하지 않는다(domain-rules: 오늘 칸만). 이미 요리한 칸은 처음 시각을 둔다
		if (!slot.slotDate().equals(LocalDate.from(now)) || slot.isCooked()) {
			return;
		}
		slot.markCooked(now);
		mealPlanSlotPort.update(slot);
	}
}
