package com.remake.recipe.adapter.out.persistence;

import java.time.LocalDateTime;

import com.remake.recipe.domain.SavedRecipe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** saved_recipes. 저장은 insert ... on conflict로 넣으므로(SavedRecipeJpaRepository) 이 엔티티로는 읽기와 즐겨찾기 변경만 한다. */
@Entity
@Table(name = "saved_recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SavedRecipeJpaEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false)
	private Long recipeId;

	// 공유 글에서 저장했으면 그 글. 공유 영역이 채운다
	private Long sharedRecipeId;

	@Column(name = "is_favorite", nullable = false)
	private boolean favorite;

	@Column(nullable = false)
	private LocalDateTime savedAt;

	SavedRecipe toDomain() {
		return new SavedRecipe(recipeId, favorite, savedAt);
	}
}
