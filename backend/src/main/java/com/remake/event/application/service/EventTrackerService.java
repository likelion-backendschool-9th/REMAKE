package com.remake.event.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.EventPropertyFilter;
import com.remake.event.domain.EventRecord;
import com.remake.event.domain.EventSource;
import com.remake.event.domain.Screen;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 EventTracker. CLIENT 이벤트·필터 거절은 조용히 무시한다.
 * 활성 트랜잭션이 있으면 커밋 뒤에만 적재해 롤백된 행동이 남지 않게 한다.
 * afterCommit·비동기 제출 실패도 삼켜서 호출 API(예: 영수증 등록) 성공 응답을 유지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventTrackerService implements EventTracker {

	private final EventWriteService eventWriteService;
	private final Clock clock;

	@Override
	public void track(EventName name, Screen screen, Map<String, Object> properties, EventTrackCommand command) {
		try {
			if (name.getSource() != EventSource.SERVER) {
				log.warn("서버 EventTracker로 CLIENT 이벤트 호출 무시: {}", name.getWireName());
				return;
			}
			var filtered = EventPropertyFilter.filter(name, properties);
			if (filtered.isEmpty()) {
				log.warn("서버 이벤트 속성 거절 name={}", name.getWireName());
				return;
			}
			EventRecord record = new EventRecord(
					name,
					command.userId(),
					command.sessionId(),
					EventSource.SERVER,
					screen == null ? null : screen.getCode(),
					filtered.get(),
					command.platform(),
					command.appVersion(),
					command.seed(),
					LocalDateTime.now(clock));
			scheduleSave(record);
		}
		catch (Exception e) {
			log.warn("서버 이벤트 발행 실패 name={}", name.getWireName(), e);
		}
	}

	private void scheduleSave(EventRecord record) {
		try {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						submitSave(record);
					}
				});
				return;
			}
			submitSave(record);
		}
		catch (Exception e) {
			log.warn("이벤트 적재 예약 실패 name={}", record.eventName().getWireName(), e);
		}
	}

	/** 프록시를 통해 @Async 제출. 큐 거부 등 제출 예외는 여기서 삼킨다. */
	private void submitSave(EventRecord record) {
		try {
			eventWriteService.saveAsync(record);
		}
		catch (Exception e) {
			log.warn("이벤트 적재 제출 실패 name={}", record.eventName().getWireName(), e);
		}
	}
}
