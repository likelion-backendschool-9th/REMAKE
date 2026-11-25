package com.remake.ingredient.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.ingredient.application.port.in.DeleteIngredientsForUserUseCase;
import com.remake.ingredient.application.port.out.DeleteIngredientsForUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteIngredientsForUserService implements DeleteIngredientsForUserUseCase {

	private final DeleteIngredientsForUserPort deleteIngredientsForUserPort;

	@Override
	@Transactional
	public void deleteIngredientsForUser(Long userId) {
		deleteIngredientsForUserPort.deleteAllByUserId(userId);
	}
}
