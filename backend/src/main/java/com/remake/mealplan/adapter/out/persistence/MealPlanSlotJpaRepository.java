package com.remake.mealplan.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 달력·하루 조회 메서드는 그 API를 만들 때 추가한다.
 * meal_type은 문자열로 저장돼 DB 정렬은 알파벳 순(BREAKFAST, DINNER, LUNCH)이 된다. 끼니 순서는 MealType 선언 순서로 정렬한다.
 */
interface MealPlanSlotJpaRepository extends JpaRepository<MealPlanSlotJpaEntity, Long> {
}
