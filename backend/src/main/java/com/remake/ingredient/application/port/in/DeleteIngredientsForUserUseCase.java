package com.remake.ingredient.application.port.in;

/**
 * 회원 탈퇴 때 부른다. 냉장고 재료와 재료 사용·폐기 기록을 지운다.
 */
public interface DeleteIngredientsForUserUseCase {

	void deleteIngredientsForUser(Long userId);
}
