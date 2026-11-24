package com.remake.recipe.application.port.out;

import java.util.Optional;

import com.remake.recipe.domain.Recipe;

/** 레시피 읽기. 소유자 필터 조회와 공유 게시글 연결용 내부 원본 조회를 제공한다. */
public interface LoadRecipePort {

	/** 재료는 화면 순서, 단계는 번호 순서로 채운다. */
	Optional<Recipe> loadById(Long recipeId);

	Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId);
}
