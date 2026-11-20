package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

/** 재료 한 건 응답. 도메인 객체를 그대로 내보내지 않고 이 모양으로 바꾼다. enum은 이름 그대로 나간다. */
public record IngredientResponse(
		Long id,
		String name,
		IngredientCategoryCode categoryCode,
		StorageLocation storage,
		LocalDate expirationDate,
		boolean isExpirationEstimated,
		BigDecimal amount,
		IngredientUnit unit,
		Long price,
		IngredientStatus status) {

	public static IngredientResponse from(Ingredient ingredient, IngredientCategoryCode categoryCode) {
		return new IngredientResponse(ingredient.id(), ingredient.name(), categoryCode, ingredient.storage(),
				ingredient.expirationDate(), ingredient.expirationEstimated(), ingredient.amount(), ingredient.unit(),
				ingredient.price(), ingredient.status());
	}
}
