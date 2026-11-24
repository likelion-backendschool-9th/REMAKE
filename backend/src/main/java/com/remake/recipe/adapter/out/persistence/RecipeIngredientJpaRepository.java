package com.remake.recipe.adapter.out.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecipeIngredientJpaRepository extends JpaRepository<RecipeIngredientJpaEntity, Long> {

	List<RecipeIngredientJpaEntity> findByRecipeIdOrderBySortOrderAsc(Long recipeId);

	List<RecipeIngredientJpaEntity> findByRecipeIdInOrderByRecipeIdAscSortOrderAsc(Collection<Long> recipeIds);
}
