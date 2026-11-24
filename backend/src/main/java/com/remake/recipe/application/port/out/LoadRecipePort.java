package com.remake.recipe.application.port.out;

import java.util.Optional;

import com.remake.recipe.domain.Recipe;

/**
 * 레시피 읽기. 소유자 필터 조회와 공유 게시글 연결용 내부 원본 조회를 제공한다.
 * 두 조회 모두 재료는 화면 순서, 단계는 번호 순서로 채운다.
 */
public interface LoadRecipePort {

	/** 공유 게시글 연결용 내부 조회. 호출하는 UseCase가 소유권이나 공개 상태를 확인한다. */
	Optional<Recipe> loadById(Long recipeId);

	/** 원본 작성자가 userId인 레시피만 조회한다. */
	Optional<Recipe> loadByIdAndUserId(Long recipeId, Long userId);
}
