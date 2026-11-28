package com.remake.share.application.port.in;

public interface DeleteSharedRecipeUseCase {
	/** 본인 공개/숨김 글 삭제. 본인 삭제 글 재호출은 204, 미존재/타인은 SHARED_RECIPE_NOT_FOUND. */
	void delete(Long userId, Long id);
}
