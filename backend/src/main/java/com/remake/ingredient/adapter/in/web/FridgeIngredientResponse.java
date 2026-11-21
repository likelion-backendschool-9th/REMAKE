package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

/** 냉장고 목록 한 줄. dDay는 오늘부터 소비기한까지 남은 일수(지났으면 음수), status는 DANGER/SOON/OK이다. */
public record FridgeIngredientResponse(
		Long id,
		String name,
		IngredientCategoryCode categoryCode,
		StorageLocation storage,
		LocalDate expirationDate,
		boolean isExpirationEstimated,
		long dDay,
		ExpiryStatus status,
		BigDecimal amount,
		IngredientUnit unit,
		Long price) {

	public static FridgeIngredientResponse from(FridgeIngredient item) {
		Ingredient i = item.ingredient();
		return new FridgeIngredientResponse(i.id(), i.name(), item.categoryCode(), i.storage(), i.expirationDate(),
				i.expirationEstimated(), item.dDay(), item.status(), i.amount(), i.unit(), i.price());
	}
}
