package com.remake.cart.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.cart.application.port.out.InsertCartItemPort;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartItemPersistenceAdapter implements LoadCartItemPort, SaveCartItemPort, InsertCartItemPort {
	private final CartItemJpaRepository repository;
	private final JdbcTemplate jdbcTemplate;

	@Override
	public Optional<CartItem> loadOwnedForUpdate(Long userId, Long cartItemId) {
		return repository.findOwnedForUpdate(userId, cartItemId).map(CartItemJpaEntity::toDomain);
	}

	@Override
	public List<CartItem> loadOwned(Long userId) {
		return repository.findByUserIdOrderById(userId).stream().map(CartItemJpaEntity::toDomain).toList();
	}

	@Override
	public List<CartItem> loadOwnedForUpdate(Long userId, List<Long> ids) {
		return repository.findOwnedForUpdate(userId, ids).stream().map(CartItemJpaEntity::toDomain).toList();
	}

	@Override
	public long countOwned(Long userId) {
		return repository.countByUserId(userId);
	}

	@Override
	public boolean insertIfAbsent(CartItem item, LocalDateTime now) {
		return jdbcTemplate.update("""
				INSERT INTO cart_items (user_id, recipe_id, meal_plan_slot_id, category_id, name, amount, unit, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (user_id, recipe_id, name, unit) WHERE recipe_id IS NOT NULL DO NOTHING
				""", item.userId(), item.recipeId(), item.mealPlanSlotId(), item.categoryId(), item.name(),
				item.amount(), item.unit().name(), now, now) > 0;
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
