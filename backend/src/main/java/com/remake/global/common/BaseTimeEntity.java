package com.remake.global.common;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 생성 시각(created_at)과 수정 시각(updated_at)이 있는 JPA 엔티티의 부모 클래스.
 * 값이 수정되는 테이블(users, ingredients, receipts, receipt_items, meal_plans, meal_plan_slots, cart_items, shared_recipes)이 상속한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
