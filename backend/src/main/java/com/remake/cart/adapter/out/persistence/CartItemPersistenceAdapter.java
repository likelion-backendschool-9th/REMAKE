package com.remake.cart.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartItemPersistenceAdapter implements LoadCartItemPort, SaveCartItemPort {
	private final CartItemJpaRepository repository;

	@Override
	public Optional<CartItem> loadOwnedForUpdate(Long userId, Long cartItemId) {
		return repository.findOwnedForUpdate(userId, cartItemId).map(CartItemJpaEntity::toDomain);
	}

	@Override
	public void save(CartItem item) {
		repository.findById(item.id()).orElseThrow().changeAmount(item.amount());
	}

	@Override
	public void delete(Long cartItemId) {
		repository.deleteById(cartItemId);
	}
}
