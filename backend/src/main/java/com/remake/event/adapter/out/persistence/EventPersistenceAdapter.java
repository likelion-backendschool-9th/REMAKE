package com.remake.event.adapter.out.persistence;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.out.SaveEventPort;
import com.remake.event.domain.EventRecord;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class EventPersistenceAdapter implements SaveEventPort {

	private final EventJpaRepository repository;
	private final Clock clock;

	@Override
	@Transactional
	public void save(EventRecord record) {
		repository.save(EventJpaEntity.from(record, LocalDateTime.now(clock)));
	}

	@Override
	@Transactional
	public void saveAll(List<EventRecord> records) {
		LocalDateTime now = LocalDateTime.now(clock);
		repository.saveAll(records.stream().map(r -> EventJpaEntity.from(r, now)).toList());
	}
}
