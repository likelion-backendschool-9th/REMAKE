package com.remake.recipe.application.port.in;

public interface GetRecipeSaveStateUseCase {
	SaveState getSaveState(Long userId, Long recipeId, Long sharedRecipeId);
	record SaveState(boolean saved, long saveCount) { }
}
