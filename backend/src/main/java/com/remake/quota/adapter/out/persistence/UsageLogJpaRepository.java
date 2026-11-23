package com.remake.quota.adapter.out.persistence;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remake.quota.domain.UsageType;

interface UsageLogJpaRepository extends JpaRepository<UsageLogJpaEntity, Long> {

	int countByUserIdAndTypeAndCreatedAtGreaterThanEqual(Long userId, UsageType type, LocalDateTime from);
}
