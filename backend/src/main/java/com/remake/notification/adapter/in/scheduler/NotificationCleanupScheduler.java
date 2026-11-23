package com.remake.notification.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.notification.application.port.in.CleanupOldNotificationsUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 새벽 3시(한국 시간) 30일 지난 알림 삭제. */
@Slf4j
@Component
@RequiredArgsConstructor
class NotificationCleanupScheduler {

	private final CleanupOldNotificationsUseCase cleanupOldNotificationsUseCase;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	void cleanupOldNotifications() {
		int deleted = cleanupOldNotificationsUseCase.cleanupOldNotifications();
		log.info("오래된 알림 정리 완료 deleted={}", deleted);
	}
}
