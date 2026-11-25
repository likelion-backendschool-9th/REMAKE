package com.remake.ingredient.adapter.out.persistence;

import java.math.BigDecimal;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.ingredient.domain.IngredientLog;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.ingredient.domain.IngredientUnit;

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
 * ingredient_logs 테이블. 만든 뒤 고치지 않는 기록이라 BaseCreatedEntity만 상속한다.
 * 테이블은 Flyway(V9__create_ingredient_logs.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * 재료와 사용자는 연관 관계 없이 ID로만 갖는다. 재료가 지워지면 DB가 ingredient_id를 비운다(ON DELETE SET NULL).
 */
@Entity
@Table(name = "ingredient_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IngredientLogJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	// 재료가 지워지면(수량 0 포함) null이 된다
	private Long ingredientId;

	// 기록한 때의 재료 카테고리. 재료를 지우거나 카테고리를 고쳐도 바뀌지 않는다.
	// 새 코드는 항상 채운다. DB는 V25에서 NULL을 허용(배포 중 구버전 쓰기)하고 후속 마이그레이션에서 NOT NULL을 건다
	@Column(nullable = false)
	private Long categoryId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientLogType type;

	// 기록한 때의 재료 이름·단위. 재료가 지워져도 기록은 이 값으로 남는다
	@Column(nullable = false, length = 50)
	private String name;

	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientUnit unit;

	// 버린 재료의 가격 비례 금액. 사용 기록이나 가격 미등록 재료는 null
	private Long costAmount;

	static IngredientLogJpaEntity from(IngredientLog log) {
		IngredientLogJpaEntity entity = new IngredientLogJpaEntity();
		entity.userId = log.userId();
		entity.ingredientId = log.ingredientId();
		entity.categoryId = log.categoryId();
		entity.type = log.type();
		entity.name = log.name();
		entity.amount = log.amount();
		entity.unit = log.unit();
		entity.costAmount = log.costAmount();
		return entity;
	}

	IngredientLog toDomain() {
		return new IngredientLog(id, userId, ingredientId, categoryId, type, name, amount, unit, costAmount);
	}
}
