package com.remake.subscription.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.subscription.domain.SubscriptionPaymentStatus;

interface SubscriptionPaymentJpaRepository extends JpaRepository<SubscriptionPaymentJpaEntity, Long> {

	Optional<SubscriptionPaymentJpaEntity> findFirstBySubscriptionIdAndStatusOrderByApprovedAtDescIdDesc(
			Long subscriptionId, SubscriptionPaymentStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SubscriptionPaymentJpaEntity p where p.userId = :userId")
	int deleteAllByUserId(@Param("userId") Long userId);
}
