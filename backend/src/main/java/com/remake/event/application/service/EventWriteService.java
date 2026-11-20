package com.remake.event.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.remake.event.application.port.out.SaveEventPort;
import com.remake.event.domain.EventRecord;
import com.remake.global.config.AsyncConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 서버 이벤트 비동기 적재.
 * 적재 실패는 로그만 남기고 호출 API를 깨뜨리지 않는다. 자동 재시도는 하지 않는다
 * (트래킹 실패가 사용자 흐름을 막지 않는 정책. 재적재는 운영·후속 배치 범위).
 */
@Slf4j
@Service
@RequiredArgsConstructor
class EventWriteService {

	private final SaveEventPort saveEventPort;

	@Async(AsyncConfig.EVENT_TASK_EXECUTOR)
	public void saveAsync(EventRecord record) {
		try {
			saveEventPort.save(record);
		}
		catch (Exception e) {
			log.warn("이벤트 적재 실패 name={}", record.eventName().getWireName(), e);
		}
	}
}
