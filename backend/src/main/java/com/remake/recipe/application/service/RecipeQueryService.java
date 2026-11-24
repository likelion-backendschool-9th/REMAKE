package com.remake.recipe.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeUseCase;
import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.domain.Recipe;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class RecipeQueryService implements GetRecipeUseCase {

	private final LoadRecipePort loadRecipePort;

	@Override
	@Transactional(readOnly = true)
	public Recipe getRecipe(Long userId, Long recipeId) {
		return loadRecipePort.loadByIdAndUserId(recipeId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
	}
}
