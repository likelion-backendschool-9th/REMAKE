package com.remake.notification.adapter.out.persistence;

import java.time.LocalDateTime;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.notification.domain.Notification;
import com.remake.notification.domain.NotificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class NotificationJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private NotificationType type;

	@Column(name = "ingredient_id")
	private Long ingredientId;

	@Column(name = "meal_plan_id")
	private Long mealPlanId;

	@Column(nullable = false, length = 500)
	private String message;

	@Column(name = "read_at")
	private LocalDateTime readAt;

	static NotificationJpaEntity from(Notification notification) {
		NotificationJpaEntity entity = new NotificationJpaEntity();
		entity.userId = notification.userId();
		entity.type = notification.type();
		entity.ingredientId = notification.ingredientId();
		entity.mealPlanId = notification.mealPlanId();
		entity.message = notification.message();
		entity.readAt = notification.readAt();
		return entity;
	}

	Notification toDomain() {
		return Notification.restore(id, userId, type, ingredientId, mealPlanId, message, readAt, getCreatedAt());
	}
}
