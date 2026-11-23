package com.remake.notification.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.notification.domain.NotificationSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
record NotificationSummaryResponse(String latestMessage, long unreadCount) {

	static NotificationSummaryResponse from(NotificationSummary summary) {
		return new NotificationSummaryResponse(summary.latestMessage(), summary.unreadCount());
	}
}
