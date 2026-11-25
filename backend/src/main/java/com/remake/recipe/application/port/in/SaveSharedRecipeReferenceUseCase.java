package com.remake.recipe.application.port.in;

/** 공유 영역에서 공개 상태와 원본 연결을 검증한 뒤 같은 트랜잭션에서 호출한다. */
public interface SaveSharedRecipeReferenceUseCase {
	/** 신규 저장일 때만 true. 기존 저장의 출처·즐겨찾기·저장 시각은 보존한다. */
	boolean saveReference(Long userId, Long recipeId, Long sharedRecipeId);
	/** 원본 기준으로 저장을 취소한다. 이미 취소된 경우도 성공한다. */
	void unsaveReference(Long userId, Long recipeId);
}
