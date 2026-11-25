package com.remake.cart.application.port.out;

public interface DeleteCartItemsForUserPort {

	void deleteAllByUserId(Long userId);
}
