package com.remake.cart.adapter.out.persistence;

import java.math.BigDecimal;

import com.remake.cart.domain.CartItem;
import com.remake.global.common.BaseTimeEntity;
import com.remake.recipe.domain.RecipeUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class CartItemJpaEntity extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@Column(nullable = false)
	private Long userId;
	private Long recipeId;
	private Long mealPlanSlotId;
	@Column(nullable = false)
	private Long categoryId;
	@Column(nullable = false, length = 50)
	private String name;
	@Column(nullable = false, precision = 10, scale = 3)
	private BigDecimal amount;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private RecipeUnit unit;

	void changeAmount(BigDecimal newAmount) {
		amount = newAmount;
	}

	CartItem toDomain() {
		return new CartItem(id, userId, recipeId, mealPlanSlotId, categoryId, name, amount, unit);
	}
}
