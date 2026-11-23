package com.remake.notification.application.port.in;

/** 매일 09:00 소비기한 알림 배치. 스케줄러가 호출한다. */
public interface RunExpiryNotificationBatchUseCase {

	void runExpiryNotificationBatch();
}
