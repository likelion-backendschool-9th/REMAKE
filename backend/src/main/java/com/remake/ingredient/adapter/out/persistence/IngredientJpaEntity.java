package com.remake.ingredient.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.global.common.BaseTimeEntity;
import com.remake.ingredient.domain.IngredientStatus;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * ingredients 테이블. 테이블은 Flyway(V2__create_ingredients.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * 다른 기능의 데이터(users, receipt_items)는 연관 관계 없이 ID로만 갖는다.
 * 등록·수정·소비 기능이 들어오는 PR에서 생성 메서드와 변경 메서드를 추가한다.
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

	// 소비기한을 추정한 값인지. 사용자가 직접 고치면 false
	@Column(name = "is_expiration_estimated", nullable = false)
	private boolean expirationEstimated;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal initialAmount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientUnit unit;

	// 구매 금액(원). 직접 추가했고 입력하지 않았으면 null
	private Long price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientStatus status;

	// 동시 수정 방지(낙관적 락)
	@Version
	private Long version;
}
