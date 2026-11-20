package com.remake.event.application.port.out;

import java.util.List;

import com.remake.event.domain.EventRecord;

/** 이벤트 원본 저장. */
public interface SaveEventPort {

	void save(EventRecord record);

	void saveAll(List<EventRecord> records);
}
