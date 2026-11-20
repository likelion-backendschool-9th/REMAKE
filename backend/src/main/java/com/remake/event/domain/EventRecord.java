package com.remake.event.domain;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 저장할 이벤트 한 건. JPA·Spring에 의존하지 않는다.
 * properties는 필터를 거친 평탄한 값만 담는다.
 */
public record EventRecord(
		EventName eventName,
		Long userId,
		String sessionId,
		EventSource source,
		String screen,
		Map<String, Object> properties,
		String platform,
		String appVersion,
		boolean seed,
		LocalDateTime occurredAt) {
}
