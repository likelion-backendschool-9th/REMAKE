package com.remake.mealplan.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remake.mealplan.domain.MealPlanStatus;

/** 달력 조회 메서드는 그 API를 만들 때 추가한다. */
interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, Long> {

	boolean existsByUserIdAndStatus(Long userId, MealPlanStatus status);
}
