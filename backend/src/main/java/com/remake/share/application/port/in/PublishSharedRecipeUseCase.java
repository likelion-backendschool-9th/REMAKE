package com.remake.share.application.port.in;

public interface PublishSharedRecipeUseCase {
	Long publish(Long userId, Long recipeId, String title, SharedRecipePhotoUpload photo);
}
