package com.remake.recipe.adapter.out.persistence;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.recipe.domain.CustomOptions;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeSource;

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
 * recipes 테이블. 테이블은 Flyway(V6__create_recipes.sql)가 만들고, JPA는 validate로 모양만 검사한다.
 * 레시피는 고치지 않으므로 created_at만 있다. 재료·단계는 따로 저장하고 어댑터가 합친다.
 */
@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecipeJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private RecipeSource source;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false)
	private int servings;

	@Column(nullable = false)
	private int cookMinutes;

	@Column(length = 1000)
	private String tips;

	// 커스텀 생성의 선택값. 빠른 생성·식단표는 null
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private CustomOptions customOptions;

	static RecipeJpaEntity from(Recipe recipe) {
		RecipeJpaEntity entity = new RecipeJpaEntity();
		entity.userId = recipe.userId();
		entity.source = recipe.source();
		entity.title = recipe.title();
		entity.servings = recipe.servings();
		entity.cookMinutes = recipe.cookMinutes();
		entity.tips = recipe.tips();
		entity.customOptions = recipe.customOptions();
		return entity;
	}
}
