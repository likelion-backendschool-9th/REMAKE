package com.remake.share.application.port.out;

public interface SaveSharedRecipePort {
	Long save(Long recipeId, String title, String imageKey);
}
