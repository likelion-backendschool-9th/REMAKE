package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.remake.global.common.BaseTimeEntity;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * meal_plan_slots 테이블. 테이블은 Flyway(V6__create_meal_plans.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * 레시피 교체·요리 표시로 값이 바뀌어 BaseTimeEntity를 상속한다.
 * 식단표(meal_plans)도 ID로만 갖는다. 식단표를 지우면 DB가 칸을 같이 지운다(ON DELETE CASCADE).
 * 레시피(recipes)는 다른 기능의 데이터라 ID로만 갖는다.
 */
@Entity
@Table(name = "meal_plan_slots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class MealPlanSlotJpaEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long mealPlanId;

	@Column(nullable = false)
	private LocalDate slotDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MealType mealType;

	@Column(nullable = false)
	private Long recipeId;

	// 오늘 칸에서 요리를 끝낸 시각. 요리하지 않았으면 null
	private LocalDateTime cookedAt;

	/** 새 칸을 저장하려고 도메인 객체에서 엔티티를 만든다. */
	static MealPlanSlotJpaEntity from(MealPlanSlot slot) {
		MealPlanSlotJpaEntity entity = new MealPlanSlotJpaEntity();
		entity.mealPlanId = slot.mealPlanId();
		entity.slotDate = slot.slotDate();
		entity.mealType = slot.mealType();
		entity.apply(slot);
		return entity;
	}

	/** 도메인 객체에서 바뀔 수 있는 값(레시피, 요리 시각)만 엔티티에 옮긴다. 변경 감지로 저장된다. */
	void apply(MealPlanSlot slot) {
		this.recipeId = slot.recipeId();
		this.cookedAt = slot.cookedAt();
	}

	MealPlanSlot toDomain() {
		return MealPlanSlot.restore(id, mealPlanId, slotDate, mealType, recipeId, cookedAt);
	}
}
