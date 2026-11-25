package com.remake.ingredient.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngredientLogJpaRepository extends JpaRepository<IngredientLogJpaEntity, Long> {

	boolean existsByIngredientId(Long ingredientId);

	void deleteByUserId(Long userId);
}
