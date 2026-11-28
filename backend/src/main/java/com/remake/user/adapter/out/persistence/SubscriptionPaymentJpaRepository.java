package com.remake.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SubscriptionPaymentJpaRepository extends JpaRepository<SubscriptionPaymentJpaEntity, Long> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from SubscriptionPaymentJpaEntity p where p.userId = :userId")
	int deleteAllByUserId(@Param("userId") Long userId);
}
