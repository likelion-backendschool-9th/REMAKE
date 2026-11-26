package com.remake.receipt.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.remake.ingredient.domain.Ingredient;
import com.remake.receipt.application.port.in.RecordManualIngredientExpenseUseCase;
import com.remake.receipt.application.port.in.UpdateManualIngredientExpensePriceUseCase;
import com.remake.receipt.application.port.out.ManualIngredientExpensePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class ManualIngredientExpenseService
		implements RecordManualIngredientExpenseUseCase, UpdateManualIngredientExpensePriceUseCase {

	private final ManualIngredientExpensePort expenses;

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void record(Ingredient ingredient, LocalDateTime registeredAt) {
		expenses.save(ingredient, registeredAt);
	}

	@Override
	@Transactional(propagation = Propagation.MANDATORY)
	public void updatePrice(Long ingredientId, Long price) {
		expenses.updatePrice(ingredientId, price);
	}
}
