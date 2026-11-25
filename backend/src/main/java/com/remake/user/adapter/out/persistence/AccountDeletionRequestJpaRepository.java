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
	@Query("""
			update AccountDeletionRequestJpaEntity a
			set a.attempts = a.attempts + 1
			where a.id = :id and a.verifiedAt is null and a.attempts < :maxAttempts
			""")
	int incrementAttemptsIfOpen(@Param("id") Long id, @Param("maxAttempts") int maxAttempts);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from AccountDeletionRequestJpaEntity a where a.createdAt < :before")
	int deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
