package com.remake.event.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.IngestClientEventsUseCase;
import com.remake.event.application.port.out.SaveEventPort;
import com.remake.event.domain.EventName;
import com.remake.event.domain.EventPropertyFilter;
import com.remake.event.domain.EventRecord;
import com.remake.event.domain.EventSource;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 프론트 배치 수집. 50개 초과는 EVENT_BATCH_TOO_LARGE.
 * 모르는 이름·SERVER 이벤트·속성 거절은 rejected. 수락분은 같은 요청에서 동기 저장해 accepted와 DB가 맞는다.
 */
@Service
@RequiredArgsConstructor
public class ClientEventIngestService implements IngestClientEventsUseCase {

	public static final int MAX_BATCH_SIZE = 50;

	private final SaveEventPort saveEventPort;
	private final Clock clock;

	@Override
	@Transactional
	public IngestResult ingest(Long userId, String platform, String appVersion, List<ClientEventCommand> events) {
		if (events == null) {
			return new IngestResult(0, 0);
		}
		if (events.size() > MAX_BATCH_SIZE) {
			throw new BusinessException(ErrorCode.EVENT_BATCH_TOO_LARGE);
		}
		List<EventRecord> accepted = new ArrayList<>();
		int rejected = 0;
		LocalDateTime now = LocalDateTime.now(clock);
		for (ClientEventCommand event : events) {
			var resolved = EventName.fromWireName(event.name());
			if (resolved.isEmpty() || resolved.get().getSource() != EventSource.CLIENT) {
				rejected++;
				continue;
			}
			EventName name = resolved.get();
			var filtered = EventPropertyFilter.filter(name, event.properties());
			if (filtered.isEmpty()) {
				rejected++;
				continue;
			}
			LocalDateTime occurredAt = event.occurredAt() != null ? event.occurredAt() : now;
			accepted.add(new EventRecord(
					name,
					userId,
					event.sessionId(),
					EventSource.CLIENT,
					blankToNull(event.screen()),
					filtered.get(),
					platform,
					appVersion,
					false,
					occurredAt));
		}
		if (!accepted.isEmpty()) {
			saveEventPort.saveAll(accepted);
		}
		return new IngestResult(accepted.size(), rejected);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}
}
