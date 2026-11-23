package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 달력·하루 조회 메서드는 그 API를 만들 때 추가한다.
 * meal_type은 문자열로 저장돼 DB 정렬은 알파벳 순(BREAKFAST, DINNER, LUNCH)이 된다. 끼니 순서는 MealType 선언 순서로 정렬한다.
 * 칸에는 회원 id가 없어 식단표(meal_plans.user_id)로 회원의 칸을 찾는다.
 */
interface MealPlanSlotJpaRepository extends JpaRepository<MealPlanSlotJpaEntity, Long> {

	@Query("""
			SELECT s FROM MealPlanSlotJpaEntity s
			WHERE s.mealPlanId IN (SELECT p.id FROM MealPlanJpaEntity p WHERE p.userId = :userId)
			  AND s.slotDate BETWEEN :from AND :to
			""")
	List<MealPlanSlotJpaEntity> findUserSlotsBetween(@Param("userId") Long userId, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query("""
			SELECT s FROM MealPlanSlotJpaEntity s
			WHERE s.mealPlanId IN (SELECT p.id FROM MealPlanJpaEntity p WHERE p.userId = :userId)
			  AND s.slotDate >= :from
			""")
	List<MealPlanSlotJpaEntity> findUserSlotsFrom(@Param("userId") Long userId, @Param("from") LocalDate from);
}
