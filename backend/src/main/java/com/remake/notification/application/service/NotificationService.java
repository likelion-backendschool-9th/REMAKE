package com.remake.notification.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.ingredient.application.port.in.GetExpiryAlertIngredientsUseCase;
import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;
import com.remake.notification.application.port.in.CleanupOldNotificationsUseCase;
import com.remake.notification.application.port.in.CreateMealPlanNotificationUseCase;
import com.remake.notification.application.port.in.GetNotificationSummaryUseCase;
import com.remake.notification.application.port.in.GetNotificationsUseCase;
import com.remake.notification.application.port.in.RunExpiryNotificationBatchUseCase;
import com.remake.notification.application.port.out.DeleteNotificationPort;
import com.remake.notification.application.port.out.LoadNotificationPort;
import com.remake.notification.application.port.out.SaveNotificationPort;
import com.remake.notification.domain.MealPlanFailReason;
import com.remake.notification.domain.Notification;
import com.remake.notification.domain.NotificationMessages;
import com.remake.notification.domain.NotificationSummary;
import com.remake.notification.domain.NotificationType;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements GetNotificationSummaryUseCase, GetNotificationsUseCase,
		RunExpiryNotificationBatchUseCase, CleanupOldNotificationsUseCase, CreateMealPlanNotificationUseCase {

	private static final int RETENTION_DAYS = 30;

	private final LoadNotificationPort loadNotificationPort;
	private final SaveNotificationPort saveNotificationPort;
	private final DeleteNotificationPort deleteNotificationPort;
	private final GetExpiryAlertIngredientsUseCase getExpiryAlertIngredientsUseCase;
	private final GetUserUseCase getUserUseCase;
	private final EventTracker eventTracker;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public NotificationSummary getSummary(Long userId) {
		String latest = loadNotificationPort.findLatestByUserId(userId)
				.map(Notification::message)
				.orElse(null);
		long unread = loadNotificationPort.countUnreadByUserId(userId);
		return new NotificationSummary(latest, unread);
	}

	@Override
	@Transactional
	public Page<Notification> getNotifications(Long userId, Pageable pageable) {
		saveNotificationPort.markAllUnreadAsRead(userId, LocalDateTime.now(clock));
		return loadNotificationPort.findByUserId(userId, pageable);
	}

	@Override
	@Transactional
	public void runExpiryNotificationBatch() {
		LocalDate today = LocalDate.now(clock);
		Map<Long, Boolean> notificationEnabledCache = new HashMap<>();

		List<ExpiringIngredient> expiring = getExpiryAlertIngredientsUseCase.getExpiringIngredients();
		for (ExpiringIngredient item : expiring) {
			if (!isNotificationEnabled(item.userId(), notificationEnabledCache)) {
				continue;
			}
			if (loadNotificationPort.existsExpirySoonOnDate(item.userId(), item.ingredientId(), today)) {
				continue;
			}
			Notification created = Notification.createExpirySoon(
					item.userId(),
					item.ingredientId(),
					NotificationMessages.expirySoon(item.name()));
			saveAndTrack(created, NotificationType.EXPIRY_SOON, 1);
		}

		List<ExpiredIngredientCount> expiredCounts = getExpiryAlertIngredientsUseCase.getExpiredIngredientCounts();
		Set<Long> usersWithNewExpired = new HashSet<>();
		for (ExpiredIngredientCount row : expiredCounts) {
			if (!isNotificationEnabled(row.userId(), notificationEnabledCache)) {
				continue;
			}
			if (loadNotificationPort.existsExpiredOnDate(row.userId(), today)) {
				continue;
			}
			Notification created = Notification.createExpired(
					row.userId(),
					NotificationMessages.expired(row.count()));
			saveAndTrack(created, NotificationType.EXPIRED, row.count());
			usersWithNewExpired.add(row.userId());
		}

		// 어제 만료 재료 이벤트는, 오늘 EXPIRED 알림을 새로 만든 회원에 대해서만 보낸다(재실행 중복 방지).
		List<ExpiredIngredient> expiredYesterday = getExpiryAlertIngredientsUseCase.getIngredientsExpiredYesterday();
		for (ExpiredIngredient item : expiredYesterday) {
			if (!usersWithNewExpired.contains(item.userId())) {
				continue;
			}
			trackIngredientExpired(item);
		}
	}

	@Override
	@Transactional
	public int cleanupOldNotifications() {
		LocalDateTime before = LocalDate.now(clock).minusDays(RETENTION_DAYS).atStartOfDay();
		return deleteNotificationPort.deleteCreatedBefore(before);
	}

	@Override
	@Transactional
	public void notifyMealPlanCompleted(Long userId, Long mealPlanId, LocalDate startDate, int generatedDays) {
		Notification created = Notification.createMealPlanCompleted(
				userId,
				mealPlanId,
				NotificationMessages.mealPlanCompleted(startDate, generatedDays));
		saveAndTrack(created, NotificationType.MEAL_PLAN_COMPLETED, 1);
	}

	@Override
	@Transactional
	public void notifyMealPlanFailed(Long userId, MealPlanFailReason reason) {
		Notification created = Notification.createMealPlanFailed(
				userId,
				NotificationMessages.mealPlanFailed(reason));
		saveAndTrack(created, NotificationType.MEAL_PLAN_FAILED, 1);
	}

	private boolean isNotificationEnabled(Long userId, Map<Long, Boolean> cache) {
		return cache.computeIfAbsent(userId, id -> getUserUseCase.getUser(id)
				.filter(User::isActive)
				.map(User::notificationEnabled)
				.orElse(false));
	}

	private void saveAndTrack(Notification notification, NotificationType type, long batchCount) {
		saveNotificationPort.save(notification);
		eventTracker.track(
				EventName.NOTIFICATION_SENT,
				Screen.UNKNOWN,
				Map.of(
						"notification_type", type.eventValue(),
						"batch_count", batchCount),
				EventTrackCommand.ofUser(notification.userId()));
	}

	private void trackIngredientExpired(ExpiredIngredient item) {
		eventTracker.track(
				EventName.INGREDIENT_EXPIRED,
				Screen.UNKNOWN,
				Map.of(
						"category_code", item.categoryCode().name(),
						"has_price", item.hasPrice()),
				EventTrackCommand.ofUser(item.userId()));
	}
}
