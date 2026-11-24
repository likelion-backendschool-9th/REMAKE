package com.remake.ingredient.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.global.common.BaseTimeEntity;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

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
 * ingredients 테이블. 테이블은 Flyway(V2__create_ingredients.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * 다른 기능의 데이터(users, receipt_items)는 연관 관계 없이 ID로만 갖는다.
 * 도메인 객체와 바꾸는 메서드는 from, apply, toDomain이다.
 */
@Entity
@Table(name = "ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IngredientJpaEntity extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long categoryId;

	// 영수증에서 등록했으면 값이 있고, 직접 추가했으면 null
	private Long receiptItemId;

	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private StorageLocation storage;

	@Column(nullable = false)
	private LocalDate expirationDate;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal initialAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientUnit unit;

	// 구매 금액(원). 직접 추가했고 입력하지 않았으면 null
	private Long price;

	/** 새 재료를 저장하려고 도메인 객체에서 엔티티를 만든다. */
	static IngredientJpaEntity from(Ingredient ingredient) {
		IngredientJpaEntity entity = new IngredientJpaEntity();
		entity.userId = ingredient.userId();
		entity.receiptItemId = ingredient.receiptItemId();
		entity.apply(ingredient);
		return entity;
	}

	/** 도메인 객체에서 바뀔 수 있는 값만 엔티티에 옮긴다. 변경 감지로 저장된다. */
	void apply(Ingredient ingredient) {
		this.categoryId = ingredient.categoryId();
		this.name = ingredient.name();
		this.unit = ingredient.unit();
		this.storage = ingredient.storage();
		this.expirationDate = ingredient.expirationDate();
		this.amount = ingredient.amount();
		this.initialAmount = ingredient.initialAmount();
		this.price = ingredient.price();
	}

	Ingredient toDomain() {
		return Ingredient.restore(id, userId, categoryId, receiptItemId, name, storage, expirationDate,
				amount, initialAmount, unit, price);
	}
}
