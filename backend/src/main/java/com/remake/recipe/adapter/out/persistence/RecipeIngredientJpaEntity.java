package com.remake.recipe.adapter.out.persistence;

import java.math.BigDecimal;

import com.remake.global.common.BaseCreatedEntity;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeUnit;

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

/** recipe_ingredients 테이블. 레시피·냉장고 재료는 연관 관계 없이 ID로만 갖는다. */
@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecipeIngredientJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long recipeId;

	// 생성할 때 쓴 내 냉장고 재료. 보충 재료이거나 재료를 지웠으면 null
	private Long ingredientId;

	@Column(nullable = false, length = 100)
	private String name;

	// null이면 "약간"
	@Column(precision = 10, scale = 3)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RecipeUnit unit;

	// 장바구니 담기용 카테고리(#76). 모르면 null
	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private IngredientCategoryCode categoryCode;

	@Column(nullable = false)
	private int sortOrder;

	static RecipeIngredientJpaEntity of(Long recipeId, RecipeIngredient ingredient, int sortOrder) {
		RecipeIngredientJpaEntity entity = new RecipeIngredientJpaEntity();
		entity.recipeId = recipeId;
		entity.ingredientId = ingredient.ingredientId();
		entity.name = ingredient.name();
		entity.amount = ingredient.amount();
		entity.unit = ingredient.unit();
		entity.categoryCode = ingredient.categoryCode();
		entity.sortOrder = sortOrder;
		return entity;
	}

	RecipeIngredient toDomain() {
		return new RecipeIngredient(ingredientId, name, amount, unit, categoryCode);
	}
}
