package com.remake.notification.domain;

/** 상단 알림 띠용. 최근 문구와 안 읽은 수. */
public record NotificationSummary(String latestMessage, long unreadCount) {

	public static NotificationSummary empty() {
		return new NotificationSummary(null, 0);
	}
}
