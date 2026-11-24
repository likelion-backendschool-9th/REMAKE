package com.remake.quota.adapter.out.persistence;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.remake.quota.application.port.out.UsageLogPort;
import com.remake.quota.domain.UsageType;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** usage_logs 읽기·쓰기와 회원 단위 잠금. */
@Component
@RequiredArgsConstructor
class UsageLogPersistenceAdapter implements UsageLogPort {

	/**
	 * PostgreSQL 권고 잠금(advisory lock)의 첫 번째 키. 기능마다 다른 값을 써서 다른 잠금과 겹치지 않게 한다.
	 * 4 = 영역 4(AI 레시피) 생성 횟수 차감.
	 */
	static final int QUOTA_LOCK_NAMESPACE = 4;

	private final UsageLogJpaRepository repository;
	private final EntityManager entityManager;

	@Override
	public int countSince(Long userId, UsageType type, LocalDateTime from) {
		return repository.countByUserIdAndTypeAndCreatedAtGreaterThanEqual(userId, type, from);
	}

	@Override
	public void save(Long userId, UsageType type, Long grantId) {
		repository.save(UsageLogJpaEntity.of(userId, type, grantId));
	}

	/**
	 * users 행을 잠그지 않고(다른 기능의 테이블) 트랜잭션 단위 권고 잠금을 건다. 트랜잭션이 끝나면 저절로 풀린다.
	 * 다른 사용자끼리는 서로 기다리지 않는다.
	 */
	@Override
	public void lockUser(Long userId) {
		entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:namespace, :userId)")
				.setParameter("namespace", QUOTA_LOCK_NAMESPACE)
				.setParameter("userId", Math.toIntExact(userId))
				.getSingleResult();
	}
}
