package com.remake.notification.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.notification.application.port.in.RunExpiryNotificationBatchUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 오전 9시(한국 시간) 소비기한 알림 배치. */
@Slf4j
@Component
@RequiredArgsConstructor
class ExpiryNotificationScheduler {

	private final RunExpiryNotificationBatchUseCase runExpiryNotificationBatchUseCase;

	@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
	void createExpiryNotifications() {
		log.info("소비기한 알림 배치 시작");
		runExpiryNotificationBatchUseCase.runExpiryNotificationBatch();
		log.info("소비기한 알림 배치 종료");
	}
}
