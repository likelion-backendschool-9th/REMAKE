package com.remake.recipe.application.port.out;

import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase.SaveState;

public interface LoadRecipeSaveStatePort {
	SaveState load(Long userId, Long recipeId, Long sharedRecipeId);
}
