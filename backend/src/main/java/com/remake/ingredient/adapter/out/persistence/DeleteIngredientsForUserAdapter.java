package com.remake.ingredient.adapter.out.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.out.DeleteIngredientsForUserPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class DeleteIngredientsForUserAdapter implements DeleteIngredientsForUserPort {

	private final IngredientLogJpaRepository ingredientLogRepository;
	private final IngredientJpaRepository ingredientRepository;
	private final JdbcClient jdbc;

	@Override
	public void deleteAllByUserId(Long userId) {
		ingredientLogRepository.deleteByUserId(userId);
		ingredientRepository.deleteByUserId(userId);
		jdbc.sql("DELETE FROM manual_ingredient_expenses WHERE user_id = :userId").param("userId", userId).update();
	}
}
