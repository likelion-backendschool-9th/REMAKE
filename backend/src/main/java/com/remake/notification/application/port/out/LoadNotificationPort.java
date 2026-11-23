package com.remake.notification.application.port.out;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.remake.notification.domain.Notification;

public interface LoadNotificationPort {

	Page<Notification> findByUserId(Long userId, Pageable pageable);

	Optional<Notification> findLatestByUserId(Long userId);

	long countUnreadByUserId(Long userId);

	boolean existsExpirySoonOnDate(Long userId, Long ingredientId, LocalDate date);

	boolean existsExpiredOnDate(Long userId, LocalDate date);
}
