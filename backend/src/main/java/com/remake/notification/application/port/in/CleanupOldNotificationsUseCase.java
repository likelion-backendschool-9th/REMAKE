package com.remake.notification.application.port.in;

/** 매일 새벽. 30일 지난 알림 삭제. */
public interface CleanupOldNotificationsUseCase {

	int cleanupOldNotifications();
}
