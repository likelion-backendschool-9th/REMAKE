package com.remake.ingredient.adapter.out.persistence;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.DeleteIngredientsForUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class DeleteIngredientsForUserAdapter implements DeleteIngredientsForUserPort {

	private final IngredientLogJpaRepository ingredientLogRepository;
	private final IngredientJpaRepository ingredientRepository;

	@Override
	public void deleteAllByUserId(Long userId) {
		ingredientLogRepository.deleteByUserId(userId);
		ingredientRepository.deleteByUserId(userId);
	}
}
