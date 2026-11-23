package com.remake.notification.adapter.in.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.common.PageResponse;
import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.notification.application.port.in.GetNotificationSummaryUseCase;
import com.remake.notification.application.port.in.GetNotificationsUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "알림")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final GetNotificationSummaryUseCase getNotificationSummaryUseCase;
	private final GetNotificationsUseCase getNotificationsUseCase;

	@Operation(summary = "알림 요약", description = "상단 알림 띠용. 가장 최근 문구와 안 읽은 수.")
	@GetMapping("/summary")
	public NotificationSummaryResponse summary(@LoginUserId Long userId) {
		return NotificationSummaryResponse.from(getNotificationSummaryUseCase.getSummary(userId));
	}

	@Operation(summary = "알림 목록", description = "최근 것부터. 이 목록을 불러오면 안 읽은 알림이 모두 읽음이 됩니다.")
	@GetMapping
	public PageResponse<NotificationResponse> list(
			@LoginUserId Long userId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
		return PageResponse.from(
				getNotificationsUseCase.getNotifications(userId, pageable),
				NotificationResponse::from);
	}
}
