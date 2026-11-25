package com.remake.mealplan.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.DeleteMealPlansForUserUseCase;
import com.remake.mealplan.application.port.out.DeleteMealPlansForUserPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteMealPlansForUserService implements DeleteMealPlansForUserUseCase {

	private final DeleteMealPlansForUserPort deleteMealPlansForUserPort;

	@Override
	@Transactional
	public void deleteMealPlansForUser(Long userId) {
		deleteMealPlansForUserPort.deleteAllByUserId(userId);
	}
}
