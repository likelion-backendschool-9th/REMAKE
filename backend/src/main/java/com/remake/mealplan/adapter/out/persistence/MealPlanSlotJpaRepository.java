package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

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

	/** userId의 식단표에 속한 칸(요리 완료 표시, #77). */
	@Query("""
			select s from MealPlanSlotJpaEntity s
			where s.id = :slotId
			  and exists (select p.id from MealPlanJpaEntity p where p.id = s.mealPlanId and p.userId = :userId)
			""")
	Optional<MealPlanSlotJpaEntity> findByIdAndUserId(@Param("slotId") Long slotId, @Param("userId") Long userId);

	/** findByIdAndUserId와 같지만 행을 잠그고 읽는다(요리 완료 표시가 식단표 덮어쓰기 저장과 겹치지 않게). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select s from MealPlanSlotJpaEntity s
			where s.id = :slotId
			  and exists (select p.id from MealPlanJpaEntity p where p.id = s.mealPlanId and p.userId = :userId)
			""")
	Optional<MealPlanSlotJpaEntity> findByIdAndUserIdForUpdate(@Param("slotId") Long slotId,
			@Param("userId") Long userId);

	/** 칸들을 잠그고 읽는다. 여러 칸을 잠글 때 순서가 엇갈리지 않게 id 순. 이미 지워진 칸은 빠진다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from MealPlanSlotJpaEntity s where s.id in :ids order by s.id")
	List<MealPlanSlotJpaEntity> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);
}
