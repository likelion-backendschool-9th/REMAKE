package com.remake.recipe.application.port.in;

import com.remake.recipe.domain.Recipe;

/** 공유 기능 내부 전용. 공유 게시글의 공개 상태 확인은 호출하는 공유 UseCase가 담당한다. 웹에 직접 노출하지 않는다. */
public interface GetRecipeForSharingUseCase {
	/** 없는 원본은 RECIPE_NOT_FOUND, 다른 작성자의 원본은 RECIPE_NOT_SHAREABLE. */
	Recipe getForPublishing(Long userId, Long recipeId);
	/** 호출자는 공개 상태를 확인한 공유 게시글의 원본 ID만 전달한다. */
	Recipe getSharedContent(Long recipeId);
}
