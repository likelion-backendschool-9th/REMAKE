package com.remake.notification.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.notification.domain.NotificationSummary;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
record NotificationSummaryResponse(@Schema(nullable = true) String latestMessage, long unreadCount) {

	static NotificationSummaryResponse from(NotificationSummary summary) {
		return new NotificationSummaryResponse(summary.latestMessage(), summary.unreadCount());
	}
}
