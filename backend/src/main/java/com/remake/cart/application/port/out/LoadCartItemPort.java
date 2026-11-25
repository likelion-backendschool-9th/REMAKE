package com.remake.cart.application.port.out;

import java.util.List;
import java.util.Optional;

import com.remake.cart.domain.CartItem;

public interface LoadCartItemPort {
	List<CartItem> loadOwned(Long userId);
	List<CartItem> loadOwnedForUpdate(Long userId, List<Long> ids);
	long countOwned(Long userId);
	Optional<CartItem> loadOwnedForUpdate(Long userId, Long cartItemId);
}
