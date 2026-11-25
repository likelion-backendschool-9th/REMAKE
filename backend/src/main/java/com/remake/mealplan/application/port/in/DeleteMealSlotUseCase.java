package com.remake.mealplan.application.port.in;

/** 식단 칸 삭제(API 41). 그 칸만 지우고 칸의 레시피와 장바구니 항목은 남긴다. */
public interface DeleteMealSlotUseCase {

	/**
	 * 없거나 다른 사용자의 칸이면 SLOT_NOT_FOUND, 지난 날짜의 칸이면 PAST_DATE_NOT_ALLOWED.
	 * 오늘 칸은 요리한 칸이어도 지울 수 있다.
	 */
	void delete(Long userId, Long slotId);
}
