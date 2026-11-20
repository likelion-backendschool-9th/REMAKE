package com.remake.global.common;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 생성 시각(created_at)만 있는 JPA 엔티티의 부모 클래스.
 * 값이 수정되지 않는 테이블(기록, 이력 등)이 상속한다. 수정되는 테이블은 {@link BaseTimeEntity}를 상속한다.
 * 시각은 JPA 감사가 ClockConfig의 Clock으로 넣는다(JpaConfig). 직접 넣지 않는다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
