package com.remake.recipe.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecipeStepJpaRepository extends JpaRepository<RecipeStepJpaEntity, Long> {

	List<RecipeStepJpaEntity> findByRecipeIdOrderByStepNoAsc(Long recipeId);
}
