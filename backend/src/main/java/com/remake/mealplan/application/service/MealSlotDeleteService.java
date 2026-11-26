package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.CleanUpMealPlanSlotCartItemsUseCase;
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
 * 이 칸에서 담은 장바구니 행은 칸을 지우기 전에 정리한다(#91): 손대지 않은 행은 지우고, 수량을 고친 행과 저장해 둔 레시피의 행은
 * 칸 연결만 비운다(CleanUpMealPlanSlotCartItemsUseCase).
 * 칸만 지우므로 식단표의 기간(generated_days)은 그대로다.
 */
@Service
@RequiredArgsConstructor
class MealSlotDeleteService implements DeleteMealSlotUseCase {

	private final MealPlanSlotPort mealPlanSlotPort;
	private final SaveMealPlanSlotPort saveMealPlanSlotPort;
	private final CleanUpMealPlanSlotCartItemsUseCase cleanUpMealPlanSlotCartItemsUseCase;
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
		// 칸을 지우면 SET NULL로 어느 칸 행인지 알 수 없게 되므로 장바구니를 먼저 정리한다(#91)
		cleanUpMealPlanSlotCartItemsUseCase.cleanUp(userId, Map.of(slot.id(), slot.recipeId()));
		saveMealPlanSlotPort.deleteAllById(List.of(slot.id()));

		// 값은 지금 검증기에 맞춰 대문자. 이벤트 값 소문자 통일은 #172에서 같이 바꾼다
		eventTracker.track(EventName.MEAL_SLOT_DELETED, Screen.MEAL_DAY, Map.of(
				"meal_type", slot.mealType().name(),
				"date_relation", slot.slotDate().equals(today) ? "TODAY" : "FUTURE"),
				EventTrackCommand.ofUser(userId));
	}
}
