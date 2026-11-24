package com.remake.notification.application.port.in;

import com.remake.notification.domain.NotificationSummary;

/** API 43. 상단 알림 띠. */
public interface GetNotificationSummaryUseCase {

	NotificationSummary getSummary(Long userId);
}
