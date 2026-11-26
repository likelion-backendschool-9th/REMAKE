package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.in.DeleteMealSlotUseCase;
import com.remake.mealplan.application.port.out.MealPlanSlotPort;
import com.remake.mealplan.application.port.out.SaveMealPlanSlotPort;
import com.remake.mealplan.domain.MealPlanSlot;

import lombok.RequiredArgsConstructor;

/**
 * 식단 칸 삭제(#175). 칸의 레시피는 지우지 않는다(레시피 상세·요리 기록에서 계속 쓴다).
 * 장바구니 항목은 cart_items.meal_plan_slot_id의 ON DELETE SET NULL로 연결만 끊기고 남는다(#57).
 * 칸만 지우므로 식단표의 기간(generated_days)은 그대로다.
 */
@Service
@RequiredArgsConstructor
class MealSlotDeleteService implements DeleteMealSlotUseCase {

	private final MealPlanSlotPort mealPlanSlotPort;
	private final SaveMealPlanSlotPort saveMealPlanSlotPort;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional
	public void delete(Long userId, Long slotId) {
		MealPlanSlot slot = mealPlanSlotPort.findByIdAndUserId(slotId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
		LocalDate today = LocalDate.now(clock);
		if (slot.isPast(today)) {
			throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
		}
		saveMealPlanSlotPort.deleteAllById(List.of(slot.id()));

		eventTracker.track(EventName.MEAL_SLOT_DELETED, Screen.MEAL_PLAN_CALENDAR, Map.of(
				"meal_type", slot.mealType().name().toLowerCase(Locale.ROOT),
				"date_relation", slot.slotDate().equals(today) ? "today" : "future"),
				EventTrackCommand.ofUser(userId));
	}
}
