package com.remake.recipe.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.recipe.application.port.in.DeleteSavedRecipesForUserUseCase;
import com.remake.recipe.application.port.out.SavedRecipePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class DeleteSavedRecipesForUserService implements DeleteSavedRecipesForUserUseCase {

	private final SavedRecipePort savedRecipePort;

	@Override
	@Transactional
	public void deleteSavedRecipesForUser(Long userId) {
		savedRecipePort.deleteAllByUserId(userId);
	}
}
