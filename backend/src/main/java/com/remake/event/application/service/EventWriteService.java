package com.remake.event.application.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.remake.event.application.port.out.SaveEventPort;
import com.remake.event.domain.EventRecord;
import com.remake.global.config.AsyncConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 서버 이벤트 비동기 적재. 예외는 삼켜서 사용자 API에 영향을 주지 않는다. */
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
