package com.remake.recipe.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 저장 레시피 목록만 지운다. 원본 레시피는 남긴다. */
public interface DeleteSavedRecipesForUserUseCase {

	void deleteSavedRecipesForUser(Long userId);
}
