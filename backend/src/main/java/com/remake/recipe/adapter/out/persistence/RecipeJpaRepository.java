package com.remake.recipe.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface RecipeJpaRepository extends JpaRepository<RecipeJpaEntity, Long> {

	Optional<RecipeJpaEntity> findByIdAndUserId(Long id, Long userId);

	boolean existsByIdAndUserId(Long id, Long userId);
}
