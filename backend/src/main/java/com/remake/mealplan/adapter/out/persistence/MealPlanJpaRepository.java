package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.mealplan.domain.MealPlanStatus;

import jakarta.persistence.LockModeType;

/** 달력 조회 메서드는 그 API를 만들 때 추가한다. */
interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, Long> {

	boolean existsByUserIdAndStatus(Long userId, MealPlanStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM MealPlanJpaEntity p WHERE p.id = :id")
	Optional<MealPlanJpaEntity> findByIdForUpdate(@Param("id") Long id);

	List<MealPlanJpaEntity> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(MealPlanStatus status,
			LocalDateTime createdAt);
}
