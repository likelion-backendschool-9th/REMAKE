package com.remake.ingredient.adapter.out.persistence;

import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
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
 * ingredient_categories 테이블. 기준 데이터라 BaseCreatedEntity·BaseTimeEntity를 상속하지 않는다.
 * 테이블은 Flyway(V1__create_ingredient_categories.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 */
@Entity
@Table(name = "ingredient_categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class IngredientCategoryJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, unique = true, length = 30)
	private IngredientCategoryCode code;

	@Column(nullable = false, length = 30)
	private String name;

	@Column(nullable = false, length = 500)
	private String imageUrl;

	// enum은 이름(STRING)으로 저장한다. 순서 번호(ORDINAL)는 쓰지 않는다.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private StorageLocation defaultStorage;

	@Column(nullable = false)
	private int defaultShelfLifeDays;

	// 카테고리 시트에 보이는 순서(ERD 부록 A)
	@Column(nullable = false)
	private int sortOrder;

	IngredientCategory toDomain() {
		return new IngredientCategory(id, code, name, imageUrl, defaultStorage, defaultShelfLifeDays);
	}
}
