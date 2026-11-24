package com.remake.cart.application.port.out;

import java.time.LocalDateTime;

import com.remake.cart.domain.CartItem;

public interface InsertCartItemPort {
	/** DB unique key로 동시 중복을 막고, 실제 한 행을 추가했을 때만 true를 반환한다. */
	boolean insertIfAbsent(CartItem item, LocalDateTime now);
}
