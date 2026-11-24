package com.remake.cart.application.port.out;

import java.util.Optional;

import com.remake.cart.domain.CartItem;

public interface LoadCartItemPort {
	Optional<CartItem> loadOwnedForUpdate(Long userId, Long cartItemId);
}
