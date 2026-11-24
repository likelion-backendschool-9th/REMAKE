package com.remake.ingredient.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remake.ingredient.domain.IngredientCategoryCode;

interface IngredientCategoryJpaRepository extends JpaRepository<IngredientCategoryJpaEntity, Long> {

	List<IngredientCategoryJpaEntity> findAllByOrderBySortOrderAsc();

	Optional<IngredientCategoryJpaEntity> findByCode(IngredientCategoryCode code);
}
