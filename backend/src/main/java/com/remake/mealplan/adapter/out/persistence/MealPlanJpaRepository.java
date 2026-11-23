package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remake.mealplan.domain.MealPlanStatus;

interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, Long> {

	boolean existsByUserIdAndStatus(Long userId, MealPlanStatus status);

	/** 달력(API 39). idx_meal_plans_user_start_date(user_id, start_date) 인덱스를 탄다. */
	List<MealPlanJpaEntity> findByUserIdAndStatusAndStartDateBetweenOrderByStartDateAscIdAsc(Long userId,
			MealPlanStatus status, LocalDate from, LocalDate to);
}
