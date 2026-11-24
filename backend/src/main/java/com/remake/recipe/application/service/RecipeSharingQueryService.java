package com.remake.recipe.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.in.GetRecipeForSharingUseCase;
import com.remake.recipe.application.port.out.LoadRecipePort;
import com.remake.recipe.domain.Recipe;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class RecipeSharingQueryService implements GetRecipeForSharingUseCase {
	private final LoadRecipePort recipes;

	@Override
	public Recipe getForPublishing(Long userId, Long recipeId) {
		Recipe recipe = recipes.loadById(recipeId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RECIPE_NOT_FOUND));
		if (!recipe.userId().equals(userId)) {
			throw new BusinessException(ErrorCode.RECIPE_NOT_SHAREABLE);
		}
		return recipe;
	}
}
