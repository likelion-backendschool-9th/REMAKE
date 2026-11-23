package com.remake.ingredient.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface IngredientCatalogJpaRepository extends JpaRepository<IngredientCatalogJpaEntity, Long> {

	Optional<IngredientCatalogJpaEntity> findByName(String name);

	List<IngredientCatalogJpaEntity> findByCategoryId(Long categoryId);
}
