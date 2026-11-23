package com.remake.recipe.adapter.out.persistence;

import com.remake.global.common.BaseCreatedEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** recipe_steps 테이블. 단계 번호는 1번부터이고 레시피 안에서 겹치지 않는다(UK). */
@Entity
@Table(name = "recipe_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RecipeStepJpaEntity extends BaseCreatedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long recipeId;

	@Column(nullable = false)
	private int stepNo;

	@Column(nullable = false, length = 1000)
	private String content;

	static RecipeStepJpaEntity of(Long recipeId, int stepNo, String content) {
		RecipeStepJpaEntity entity = new RecipeStepJpaEntity();
		entity.recipeId = recipeId;
		entity.stepNo = stepNo;
		entity.content = content;
		return entity;
	}
}
