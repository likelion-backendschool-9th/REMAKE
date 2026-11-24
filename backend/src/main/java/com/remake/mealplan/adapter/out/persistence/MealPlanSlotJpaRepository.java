package com.remake.mealplan.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 달력·하루 조회 메서드는 그 API를 만들 때 추가한다.
 * meal_type은 문자열로 저장돼 DB 정렬은 알파벳 순(BREAKFAST, DINNER, LUNCH)이 된다. 끼니 순서는 MealType 선언 순서로 정렬한다.
 */
interface MealPlanSlotJpaRepository extends JpaRepository<MealPlanSlotJpaEntity, Long> {

	/** userId의 식단표에 속한 칸(요리 완료 표시, #77). */
	@Query("""
			select s from MealPlanSlotJpaEntity s
			where s.id = :slotId
			  and exists (select p.id from MealPlanJpaEntity p where p.id = s.mealPlanId and p.userId = :userId)
			""")
	Optional<MealPlanSlotJpaEntity> findByIdAndUserId(@Param("slotId") Long slotId, @Param("userId") Long userId);
}
