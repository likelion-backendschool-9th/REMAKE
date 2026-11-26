package com.remake.cart.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import com.remake.cart.application.port.out.DeleteCartItemsForUserPort;
import com.remake.cart.application.port.out.InsertCartItemPort;
import com.remake.cart.application.port.out.LoadCartItemPort;
import com.remake.cart.application.port.out.MealPlanSlotCartItemPort;
import com.remake.cart.application.port.out.SaveCartItemPort;
import com.remake.cart.domain.CartItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CartItemPersistenceAdapter
		implements LoadCartItemPort, SaveCartItemPort, InsertCartItemPort, DeleteCartItemsForUserPort,
		MealPlanSlotCartItemPort {
	private final CartItemJpaRepository repository;
	private final JdbcTemplate jdbcTemplate;
	private final NamedParameterJdbcTemplate namedJdbcTemplate;

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
	public List<SlotCartRow> lockBySlots(Long userId, Collection<Long> mealPlanSlotIds) {
		if (mealPlanSlotIds.isEmpty()) {
			return List.of();
		}
		return namedJdbcTemplate.query("""
				SELECT id, recipe_id, meal_plan_slot_id, created_at = updated_at AS untouched
				FROM cart_items
				WHERE user_id = :userId AND meal_plan_slot_id IN (:slotIds)
				ORDER BY id
				FOR UPDATE
				""", Map.of("userId", userId, "slotIds", mealPlanSlotIds),
				(row, number) -> new SlotCartRow(row.getLong("id"), row.getObject("recipe_id", Long.class),
						row.getLong("meal_plan_slot_id"), row.getBoolean("untouched")));
	}

	@Override
	public void deleteAllById(Collection<Long> cartItemIds) {
		namedJdbcTemplate.update("DELETE FROM cart_items WHERE id IN (:ids)", Map.of("ids", cartItemIds));
	}

	@Override
	public void unlinkSlot(Collection<Long> cartItemIds) {
		// updated_at은 그대로 둔다: 칸 연결 해제는 사용자의 수량 수정이 아니다
		namedJdbcTemplate.update("UPDATE cart_items SET meal_plan_slot_id = NULL WHERE id IN (:ids)",
				Map.of("ids", cartItemIds));
	}

	@Override
	public void save(CartItem item) {
		repository.findById(item.id()).orElseThrow().changeAmount(item.amount());
	}

	@Override
	public void delete(Long cartItemId) {
		repository.deleteById(cartItemId);
	}

	@Override
	public void deleteAllByUserId(Long userId) {
		repository.deleteByUserId(userId);
	}
}
