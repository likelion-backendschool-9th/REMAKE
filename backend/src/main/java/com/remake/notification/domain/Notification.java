package com.remake.notification.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/** 회원에게 만든 앱 안 알림 한 건. */
public final class Notification {

	private final Long id;
	private final Long userId;
	private final NotificationType type;
	private final Long ingredientId;
	private final Long mealPlanId;
	private final String message;
	private LocalDateTime readAt;
	private final LocalDateTime createdAt;

	private Notification(Long id, Long userId, NotificationType type, Long ingredientId, Long mealPlanId,
			String message, LocalDateTime readAt, LocalDateTime createdAt) {
		this.id = id;
		this.userId = Objects.requireNonNull(userId, "userId");
		this.type = Objects.requireNonNull(type, "type");
		this.ingredientId = ingredientId;
		this.mealPlanId = mealPlanId;
		this.message = Objects.requireNonNull(message, "message");
		this.readAt = readAt;
		this.createdAt = createdAt;
	}

	public static Notification createExpirySoon(Long userId, Long ingredientId, String message) {
		Objects.requireNonNull(ingredientId, "ingredientId");
		return new Notification(null, userId, NotificationType.EXPIRY_SOON, ingredientId, null, message, null, null);
	}

	public static Notification createExpired(Long userId, String message) {
		return new Notification(null, userId, NotificationType.EXPIRED, null, null, message, null, null);
	}

	public static Notification createMealPlanCompleted(Long userId, Long mealPlanId, String message) {
		Objects.requireNonNull(mealPlanId, "mealPlanId");
		return new Notification(null, userId, NotificationType.MEAL_PLAN_COMPLETED, null, mealPlanId, message, null,
				null);
	}

	public static Notification createMealPlanFailed(Long userId, String message) {
		return new Notification(null, userId, NotificationType.MEAL_PLAN_FAILED, null, null, message, null, null);
	}

	public static Notification restore(Long id, Long userId, NotificationType type, Long ingredientId, Long mealPlanId,
			String message, LocalDateTime readAt, LocalDateTime createdAt) {
		return new Notification(id, userId, type, ingredientId, mealPlanId, message, readAt, createdAt);
	}

	public void markRead(LocalDateTime readAt) {
		if (this.readAt == null) {
			this.readAt = Objects.requireNonNull(readAt, "readAt");
		}
	}

	public boolean isUnread() {
		return readAt == null;
	}

	/** 목록 응답의 mealPlanId. 완성 알림만 값을 주고 나머지는 null. */
	public Long mealPlanIdForResponse() {
		return type == NotificationType.MEAL_PLAN_COMPLETED ? mealPlanId : null;
	}

	public Long id() {
		return id;
	}

	public Long userId() {
		return userId;
	}

	public NotificationType type() {
		return type;
	}

	public Long ingredientId() {
		return ingredientId;
	}

	public Long mealPlanId() {
		return mealPlanId;
	}

	public String message() {
		return message;
	}

	public LocalDateTime readAt() {
		return readAt;
	}

	public LocalDateTime createdAt() {
		return createdAt;
	}
}
