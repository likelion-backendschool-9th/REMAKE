package com.remake.event.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.remake.event.domain.EventRecord;
import com.remake.event.domain.EventSource;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * events 테이블. BaseCreatedEntity를 상속하지 않고 occurred_at·created_at을 직접 넣는다.
 * 테이블은 Flyway(V3__create_events.sql)가 만든다.
 */
@Entity
@Table(name = "events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class EventJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_name", nullable = false, length = 100)
	private String eventName;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "session_id", length = 100)
	private String sessionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private EventSource source;

	@Column(length = 100)
	private String screen;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private Map<String, Object> properties;

	@Column(length = 10)
	private String platform;

	@Column(name = "app_version", length = 50)
	private String appVersion;

	@Column(name = "is_seed", nullable = false)
	private boolean seed;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	static EventJpaEntity from(EventRecord record, LocalDateTime createdAt) {
		EventJpaEntity entity = new EventJpaEntity();
		entity.eventName = record.eventName().getWireName();
		entity.userId = record.userId();
		entity.sessionId = record.sessionId();
		entity.source = record.source();
		entity.screen = record.screen();
		entity.properties = record.properties().isEmpty() ? null : record.properties();
		entity.platform = record.platform();
		entity.appVersion = record.appVersion();
		entity.seed = record.seed();
		entity.occurredAt = record.occurredAt();
		entity.createdAt = createdAt;
		return entity;
	}
}
