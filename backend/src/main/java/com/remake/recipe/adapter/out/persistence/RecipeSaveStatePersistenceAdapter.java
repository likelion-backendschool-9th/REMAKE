package com.remake.recipe.adapter.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.recipe.application.port.in.GetRecipeSaveStateUseCase.SaveState;
import com.remake.recipe.application.port.out.LoadRecipeSaveStatePort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RecipeSaveStatePersistenceAdapter implements LoadRecipeSaveStatePort {
	private final JdbcTemplate jdbc;
	@Override
	public SaveState load(Long userId, Long recipeId, Long sharedRecipeId) {
		return jdbc.queryForObject("""
				SELECT EXISTS(SELECT 1 FROM saved_recipes WHERE user_id = ? AND recipe_id = ?) AS saved,
					   (SELECT COUNT(*) FROM saved_recipes WHERE shared_recipe_id = ?) AS save_count
				""", (row, number) -> new SaveState(row.getBoolean("saved"), row.getLong("save_count")),
				userId, recipeId, sharedRecipeId);
	}
}
