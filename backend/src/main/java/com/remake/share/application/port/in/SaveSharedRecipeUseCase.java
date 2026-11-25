package com.remake.share.application.port.in;

public interface SaveSharedRecipeUseCase {
	SaveResult save(Long userId, Long sharedRecipeId);
	UnsaveResult unsave(Long userId, Long sharedRecipeId);

	record SaveResult(long saveCount, boolean saved, int cartAddedCount) { }
	record UnsaveResult(long saveCount, boolean saved) { }
}
