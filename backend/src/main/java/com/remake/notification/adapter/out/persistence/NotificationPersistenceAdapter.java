package com.remake.notification.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.remake.notification.application.port.out.DeleteNotificationPort;
import com.remake.notification.application.port.out.LoadNotificationPort;
import com.remake.notification.application.port.out.SaveNotificationPort;
import com.remake.notification.domain.Notification;
import com.remake.notification.domain.NotificationType;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class NotificationPersistenceAdapter implements LoadNotificationPort, SaveNotificationPort, DeleteNotificationPort {

	private final NotificationJpaRepository notificationJpaRepository;

	@Override
	public Page<Notification> findByUserId(Long userId, Pageable pageable) {
		return notificationJpaRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable)
				.map(NotificationJpaEntity::toDomain);
	}

	@Override
	public Optional<Notification> findLatestByUserId(Long userId) {
		return notificationJpaRepository.findFirstByUserIdOrderByCreatedAtDescIdDesc(userId)
				.map(NotificationJpaEntity::toDomain);
	}

	@Override
	public long countUnreadByUserId(Long userId) {
		return notificationJpaRepository.countByUserIdAndReadAtIsNull(userId);
	}

	@Override
	public boolean existsExpirySoonOnDate(Long userId, Long ingredientId, LocalDate date) {
		return notificationJpaRepository.existsExpirySoonOnDay(
				userId,
				NotificationType.EXPIRY_SOON,
				ingredientId,
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());
	}

	@Override
	public boolean existsExpiredOnDate(Long userId, LocalDate date) {
		return notificationJpaRepository.existsTypeOnDay(
				userId,
				NotificationType.EXPIRED,
				date.atStartOfDay(),
				date.plusDays(1).atStartOfDay());
	}

	@Override
	public Notification save(Notification notification) {
		NotificationJpaEntity saved = notificationJpaRepository.save(NotificationJpaEntity.from(notification));
		return saved.toDomain();
	}

	@Override
	public int markAllUnreadAsRead(Long userId, LocalDateTime readAt) {
		return notificationJpaRepository.markAllUnreadAsRead(userId, readAt);
	}

	@Override
	public int deleteCreatedBefore(LocalDateTime before) {
		return notificationJpaRepository.deleteByCreatedAtBefore(before);
	}

	@Override
	public int deleteByUserId(Long userId) {
		return notificationJpaRepository.deleteByUserId(userId);
	}
}
