package com.remake.notification.application.port.out;

import java.time.LocalDateTime;

import com.remake.notification.domain.Notification;

public interface SaveNotificationPort {

	Notification save(Notification notification);

	/** 회원의 안 읽은 알림 모두에 read_at을 넣는다. 이미 읽은 것은 건드리지 않는다. */
	int markAllUnreadAsRead(Long userId, LocalDateTime readAt);
}
