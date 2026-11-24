package com.remake.recipe.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase;
import com.remake.recipe.application.port.out.LoadRecipeSaveStatePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class RecipeSaveStateService implements GetRecipeSaveStateUseCase {
	private final LoadRecipeSaveStatePort saves;
	@Override
	public SaveState getSaveState(Long userId, Long recipeId, Long sharedRecipeId) {
		return saves.load(userId, recipeId, sharedRecipeId);
	}
}
