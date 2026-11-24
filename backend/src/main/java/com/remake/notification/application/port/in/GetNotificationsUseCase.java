package com.remake.notification.application.port.in;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.remake.notification.domain.Notification;

/**
 * API 44. 알림 목록. 불러오면 안 읽은 알림을 모두 읽음으로 바꾼다.
 */
public interface GetNotificationsUseCase {

	Page<Notification> getNotifications(Long userId, Pageable pageable);
}
