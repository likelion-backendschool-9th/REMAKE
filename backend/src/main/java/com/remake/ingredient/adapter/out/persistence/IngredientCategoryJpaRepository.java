package com.remake.ingredient.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngredientCategoryJpaRepository extends JpaRepository<IngredientCategoryJpaEntity, Long> {

	List<IngredientCategoryJpaEntity> findAllByOrderByIdAsc();
}
