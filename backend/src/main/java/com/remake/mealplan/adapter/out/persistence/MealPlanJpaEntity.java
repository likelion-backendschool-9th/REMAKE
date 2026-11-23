package com.remake.mealplan.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Set;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanStatus;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * meal_plans 테이블. 테이블은 Flyway(V6__create_meal_plans.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * ERD v4에 updated_at이 없어 BaseCreatedEntity를 상속한다(상태가 바뀌어도 수정 시각은 남기지 않는다).
 * 회원(users)은 연관 관계 없이 ID로만 갖는다. 도메인 객체와 바꾸는 메서드는 from, apply, toDomain이다.
 */
@Entity
@Table(name = "meal_plans")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class MealPlanJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private LocalDate startDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MealPurpose purpose;

	@Convert(converter = MealTypesConverter.class)
	@Column(nullable = false, length = 30)
	private Set<MealType> mealTypes;

	@Column(nullable = false)
	private int servings;

	// 실제로 만든 일수. 완성 전에는 null
	private Integer generatedDays;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MealPlanStatus status;

	/** 새 식단표를 저장하려고 도메인 객체에서 엔티티를 만든다. */
	static MealPlanJpaEntity from(MealPlan mealPlan) {
		MealPlanJpaEntity entity = new MealPlanJpaEntity();
		entity.userId = mealPlan.userId();
		entity.startDate = mealPlan.startDate();
		entity.purpose = mealPlan.purpose();
		entity.mealTypes = mealPlan.mealTypes();
		entity.servings = mealPlan.servings();
		entity.apply(mealPlan);
		return entity;
	}

	/** 도메인 객체에서 바뀔 수 있는 값(작업 결과)만 엔티티에 옮긴다. 변경 감지로 저장된다. */
	void apply(MealPlan mealPlan) {
		this.generatedDays = mealPlan.generatedDays();
		this.status = mealPlan.status();
	}

	MealPlan toDomain() {
		return MealPlan.restore(id, userId, startDate, purpose, mealTypes, servings, generatedDays, status);
	}
}
