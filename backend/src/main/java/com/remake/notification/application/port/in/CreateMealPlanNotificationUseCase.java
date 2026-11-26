package com.remake.notification.application.port.in;

import java.time.LocalDate;

import com.remake.notification.domain.MealPlanFailReason;

/**
 * 식단표 생성 작업(05)이 끝날 때 호출. 알림 받기 OFF여도 만든다.
 */
public interface CreateMealPlanNotificationUseCase {

	/**
	 * @param maxDays        접수 때 요금제의 최대 일수. 생성 일수가 이보다 적으면 부족 문장을 덧붙인다
	 * @param cartAddedCount 부족 재료로 장바구니에 담은 행 수
	 */
	void notifyMealPlanCompleted(Long userId, Long mealPlanId, LocalDate startDate, int generatedDays, int maxDays,
			int cartAddedCount);

	void notifyMealPlanFailed(Long userId, MealPlanFailReason reason);
}
