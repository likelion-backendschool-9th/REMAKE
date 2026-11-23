package com.remake.notification.application.port.in;

import java.time.LocalDate;

import com.remake.notification.domain.MealPlanFailReason;

/**
 * 식단표 생성 작업(05)이 끝날 때 호출. 알림 받기 OFF여도 만든다.
 */
public interface CreateMealPlanNotificationUseCase {

	void notifyMealPlanCompleted(Long userId, Long mealPlanId, LocalDate startDate, int generatedDays);

	void notifyMealPlanFailed(Long userId, MealPlanFailReason reason);
}
