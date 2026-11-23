package com.remake.mealplan.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** 조회 메서드는 이 저장소를 쓰는 API를 만들 때 추가한다. */
interface MealPlanJpaRepository extends JpaRepository<MealPlanJpaEntity, Long> {
}
