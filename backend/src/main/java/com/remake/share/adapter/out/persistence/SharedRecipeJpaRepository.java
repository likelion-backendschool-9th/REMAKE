package com.remake.share.adapter.out.persistence;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

interface SharedRecipeJpaRepository extends JpaRepository<SharedRecipeJpaEntity, Long> {
	boolean existsByRecipeIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
			Long recipeId, LocalDateTime start, LocalDateTime end);
}
