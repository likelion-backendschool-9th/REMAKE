package com.remake.cart.application.port.out;

import com.remake.cart.domain.CartItem;

public interface SaveCartItemPort {
	void save(CartItem item);
	void delete(Long cartItemId);
}
