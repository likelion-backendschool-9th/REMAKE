package com.remake.recipe.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RecipeIngredientJpaRepository extends JpaRepository<RecipeIngredientJpaEntity, Long> {

	List<RecipeIngredientJpaEntity> findByRecipeIdOrderBySortOrderAsc(Long recipeId);

	/** 사용자 본인의 레시피 재료만. 레시피 id 순, 같은 레시피 안에서는 화면 순서. */
	@Query("""
			SELECT i FROM RecipeIngredientJpaEntity i
			WHERE i.recipeId IN (
			    SELECT r.id FROM RecipeJpaEntity r WHERE r.id IN :recipeIds AND r.userId = :userId)
			ORDER BY i.recipeId, i.sortOrder
			""")
	List<RecipeIngredientJpaEntity> findAllOfUserRecipes(@Param("recipeIds") Collection<Long> recipeIds,
			@Param("userId") Long userId);
}
