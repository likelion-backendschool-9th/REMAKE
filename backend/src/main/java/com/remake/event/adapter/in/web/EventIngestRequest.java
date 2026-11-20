package com.remake.event.adapter.in.web;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * POST /api/v1/user/events 요청 본문.
 * 배치 크기 상한(50)은 서비스에서 EVENT_BATCH_TOO_LARGE로 검사한다(@Size는 400이라 쓰지 않음).
 */
public record EventIngestRequest(@NotNull @Valid List<EventItem> events) {

	public record EventItem(
			@NotBlank String name,
			String screen,
			Map<String, Object> properties,
			LocalDateTime occurredAt,
			String sessionId) {
	}
}
