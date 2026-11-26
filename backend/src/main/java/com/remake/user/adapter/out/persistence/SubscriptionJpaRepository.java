package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.user.domain.SubscriptionStatus;

interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, Long> {

	@Query("""
			select s from SubscriptionJpaEntity s
			where s.userId = :userId and s.status in :statuses
			""")
	Optional<SubscriptionJpaEntity> findByUserIdAndStatusIn(
			@Param("userId") Long userId, @Param("statuses") Collection<SubscriptionStatus> statuses);

	Optional<SubscriptionJpaEntity> findByCustomerKey(String customerKey);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update SubscriptionJpaEntity s
			set s.status = com.remake.user.domain.SubscriptionStatus.ENDED,
			    s.billingKey = null,
			    s.endedAt = :endedAt
			where s.id = :id and s.status in :statuses
			""")
	int endUnfinished(
			@Param("id") Long id,
			@Param("endedAt") LocalDateTime endedAt,
			@Param("statuses") Collection<SubscriptionStatus> statuses);
}
