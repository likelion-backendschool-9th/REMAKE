package com.remake.user.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserExcludedIngredientJpaRepository
		extends JpaRepository<UserExcludedIngredientJpaEntity, UserExcludedIngredientJpaEntity.Pk> {

	List<UserExcludedIngredientJpaEntity> findByUserIdOrderByAllergenCodeAsc(Long userId);

	int countByUserId(Long userId);

	void deleteByUserId(Long userId);
}
