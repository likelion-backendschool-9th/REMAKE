package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.mealplan.domain.MealPlanStatus;

import jakarta.persistence.LockModeType;

interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, Long> {

	boolean existsByUserIdAndStatus(Long userId, MealPlanStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT p FROM MealPlanJpaEntity p WHERE p.id = :id")
	Optional<MealPlanJpaEntity> findByIdForUpdate(@Param("id") Long id);

	List<MealPlanJpaEntity> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(MealPlanStatus status,
			LocalDateTime createdAt);

	/** 달력(API 39). idx_meal_plans_user_start_date(user_id, start_date) 인덱스를 탄다. */
	List<MealPlanJpaEntity> findByUserIdAndStatusAndStartDateBetweenOrderByStartDateAscIdAsc(Long userId,
			MealPlanStatus status, LocalDate from, LocalDate to);

	void deleteByUserId(Long userId);
}
