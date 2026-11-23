package com.remake.quota.adapter.out.persistence;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.quota.domain.UsageType;

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

/** usage_logs 테이블(V7). 성공한 생성 한 건. 고치지 않으므로 created_at(성공 시각)만 있다. */
@Entity
@Table(name = "usage_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class UsageLogJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private UsageType type;

	// 보상 횟수를 썼으면 그 지급 건. 기본 횟수면 null
	private Long grantId;

	static UsageLogJpaEntity of(Long userId, UsageType type, Long grantId) {
		UsageLogJpaEntity entity = new UsageLogJpaEntity();
		entity.userId = userId;
		entity.type = type;
		entity.grantId = grantId;
		return entity;
	}
}
