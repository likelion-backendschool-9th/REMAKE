package com.remake.share.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

interface SharedRecipeJpaRepository extends JpaRepository<SharedRecipeJpaEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from SharedRecipeJpaEntity p where p.id = :id")
	Optional<SharedRecipeJpaEntity> findByIdForUpdate(Long id);

	boolean existsByRecipeIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			Long recipeId, LocalDateTime start, LocalDateTime end);
}
