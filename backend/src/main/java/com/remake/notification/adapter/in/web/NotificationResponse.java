package com.remake.notification.adapter.in.web;

import java.time.LocalDateTime;

import com.remake.notification.domain.Notification;
import com.remake.notification.domain.NotificationType;

import io.swagger.v3.oas.annotations.media.Schema;

// mealPlanId는 MEAL_PLAN_COMPLETED가 아니면 null로 내려간다(API 44 명세).
record NotificationResponse(
		Long id,
		NotificationType type,
		String message,
		@Schema(nullable = true) Long mealPlanId,
		LocalDateTime createdAt) {

	static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.id(),
				notification.type(),
				notification.message(),
				notification.mealPlanIdForResponse(),
				notification.createdAt());
	}
}
