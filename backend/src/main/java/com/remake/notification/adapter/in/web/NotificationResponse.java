package com.remake.notification.adapter.in.web;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.notification.domain.Notification;
import com.remake.notification.domain.NotificationType;

@JsonInclude(JsonInclude.Include.NON_NULL)
record NotificationResponse(
		Long id,
		NotificationType type,
		String message,
		Long mealPlanId,
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
