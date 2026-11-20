package com.remake.event.application.port.in;

import java.util.List;

/**
 * 프론트 배치 이벤트 수집. 최대 50개. 모르는 이름·필터 거절은 rejected로 센다.
 */
public interface IngestClientEventsUseCase {

	IngestResult ingest(Long userId, String platform, String appVersion, List<ClientEventCommand> events);

	record ClientEventCommand(
			String name,
			String screen,
			java.util.Map<String, Object> properties,
			java.time.LocalDateTime occurredAt,
			String sessionId) {
	}

	record IngestResult(int accepted, int rejected) {
	}
}
