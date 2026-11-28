package com.remake.share.adapter.out.persistence;

import com.remake.global.common.BaseTimeEntity;
import com.remake.share.domain.SharedRecipe;
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

@Entity
@Table(name = "shared_recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SharedRecipeJpaEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private Long recipeId;
	@Column(columnDefinition = "text")
	private String title;
	@Column
	private String imageKey;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SharedRecipe.Status status;

	static SharedRecipeJpaEntity create(Long recipeId, String title, String imageKey) {
		SharedRecipeJpaEntity e = new SharedRecipeJpaEntity();
		e.recipeId = recipeId;
		e.title = title;
		e.imageKey = imageKey;
		e.status = SharedRecipe.Status.VISIBLE;
		return e;
	}
	SharedRecipe toDomain() {
		return new SharedRecipe(id, recipeId, title, imageKey, status, getCreatedAt());
	}
}
