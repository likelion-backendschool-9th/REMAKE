package com.remake.cart.application.port.in;

import java.math.BigDecimal;

import com.remake.cart.domain.CartItem;

public interface UpdateCartItemUseCase {
	CartItem updateAmount(Long userId, Long cartItemId, BigDecimal amount);
}
