package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AccountDeletionRequestJpaRepository extends JpaRepository<AccountDeletionRequestJpaEntity, Long> {

	Optional<AccountDeletionRequestJpaEntity> findFirstByUserIdAndVerifiedAtIsNullOrderByCreatedAtDescIdDesc(
			Long userId);

	void deleteByUserId(Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from AccountDeletionRequestJpaEntity a where a.createdAt < :before")
	int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
