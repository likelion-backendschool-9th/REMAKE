package com.remake.recipe.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.recipe.application.port.in.SaveSharedRecipeReferenceUseCase;
import com.remake.recipe.application.port.out.SavedRecipePort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
class SharedRecipeReferenceService implements SaveSharedRecipeReferenceUseCase {
	private final SavedRecipePort saves;
	private final Clock clock;

	@Override
	public boolean saveReference(Long userId, Long recipeId, Long sharedRecipeId) {
		return saves.insertSharedIfAbsent(userId, recipeId, sharedRecipeId, LocalDateTime.now(clock));
	}

	@Override
	public void unsaveReference(Long userId, Long recipeId) {
		saves.delete(userId, recipeId);
	}
}
