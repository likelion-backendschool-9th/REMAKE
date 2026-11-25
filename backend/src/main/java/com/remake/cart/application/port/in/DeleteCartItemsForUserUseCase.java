package com.remake.cart.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 장바구니 행을 모두 지운다. */
public interface DeleteCartItemsForUserUseCase {

	void deleteCartItemsForUser(Long userId);
}
