package com.remake.ingredient.adapter.out.persistence;

import com.remake.ingredient.domain.IngredientCatalogItem;
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
 * ingredient_catalog 테이블. 기준 데이터라 BaseCreatedEntity·BaseTimeEntity를 상속하지 않는다.
 * 테이블은 Flyway(V5__create_ingredient_catalog.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 */
@Entity
@Table(name = "ingredient_catalog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IngredientCatalogJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long categoryId;

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private IngredientUnit unit;

	// null이면 카테고리 기본 보관 일수
	private Integer shelfLifeDays;

	@Column(name = "is_common_exclusion", nullable = false)
	private boolean commonExclusion;

	IngredientCatalogItem toDomain() {
		return new IngredientCatalogItem(id, categoryId, name, unit, shelfLifeDays, commonExclusion);
	}
}
