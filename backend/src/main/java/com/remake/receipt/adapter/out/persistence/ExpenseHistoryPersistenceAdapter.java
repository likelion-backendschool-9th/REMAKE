package com.remake.receipt.adapter.out.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.ExpenseHistoryUseCase.Item;
import com.remake.receipt.application.port.out.LoadExpenseHistoryPort;
import com.remake.receipt.application.port.out.ManualIngredientExpensePort;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
class ExpenseHistoryPersistenceAdapter implements LoadExpenseHistoryPort, ManualIngredientExpensePort {

	private final JdbcClient jdbc;

	@Override
	public List<Entry> findBetween(Long userId, LocalDate from, LocalDate to) {
		return jdbc.sql("""
				SELECT e.purchased_on, e.name, c.code, c.image_url, e.storage, e.amount, e.unit, e.price
				  FROM (
				    SELECT r.purchased_on, i.category_id, i.name, i.storage, i.amount, i.unit, i.price,
				           0 AS source_order, i.id AS item_id
				      FROM receipts r JOIN receipt_items i ON i.receipt_id = r.id
				     WHERE r.user_id = :userId AND r.purchased_on >= :from AND r.purchased_on < :to
				       AND i.price IS NOT NULL
				    UNION ALL
				    SELECT CAST(m.created_at AS date) AS purchased_on, m.category_id, m.name, m.storage, m.amount, m.unit, m.price,
				           1 AS source_order, m.ingredient_id AS item_id
				      FROM manual_ingredient_expenses m
				     WHERE m.user_id = :userId AND m.created_at >= :fromTime AND m.created_at < :toTime
					       AND m.price IS NOT NULL
				  ) e JOIN ingredient_categories c ON c.id = e.category_id
				 ORDER BY e.purchased_on, e.source_order, e.item_id
				""")
				.param("userId", userId)
				.param("from", from)
				.param("to", to)
				.param("fromTime", from.atStartOfDay())
				.param("toTime", to.atStartOfDay())
				.query((rs, rowNum) -> new Entry(rs.getObject("purchased_on", LocalDate.class),
						new Item(rs.getString("name"), IngredientCategoryCode.valueOf(rs.getString("code")),
								rs.getString("image_url"), StorageLocation.valueOf(rs.getString("storage")),
								rs.getBigDecimal("amount"), IngredientUnit.valueOf(rs.getString("unit")), rs.getLong("price"))))
				.list();
	}

	@Override
	public void save(Ingredient ingredient, LocalDateTime registeredAt) {
		jdbc.sql("""
				INSERT INTO manual_ingredient_expenses
				    (ingredient_id, user_id, category_id, name, storage, amount, unit, price, created_at)
				VALUES (:id, :userId, :categoryId, :name, :storage, :amount, :unit, :price, :createdAt)
				""")
				.param("id", ingredient.id())
				.param("userId", ingredient.userId())
				.param("categoryId", ingredient.categoryId())
				.param("name", ingredient.name())
				.param("storage", ingredient.storage().name())
				.param("amount", ingredient.initialAmount())
				.param("unit", ingredient.unit().name())
				.param("price", ingredient.price())
				.param("createdAt", registeredAt)
				.update();
	}

	@Override
	public void updatePrice(Long ingredientId, Long price) {
		jdbc.sql("""
				UPDATE manual_ingredient_expenses
				   SET price = :price
				 WHERE ingredient_id = :ingredientId
				""")
				.param("price", price)
				.param("ingredientId", ingredientId)
				.update();
	}

	@Override
	public void deleteAllForUser(Long userId) {
		jdbc.sql("DELETE FROM manual_ingredient_expenses WHERE user_id = :userId")
				.param("userId", userId)
				.update();
	}
}
