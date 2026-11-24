package com.remake.ingredient.domain;

import java.time.LocalDate;

/** 냉장고 목록·상세의 재료 한 건. 재료에 카테고리와 오늘 기준 D-day·상태를 붙였다. dDay가 음수면 기한이 지난 것이다. */
public record FridgeIngredient(Ingredient ingredient, IngredientCategory category, long dDay, ExpiryStatus status) {

	/** D-day와 상태는 ExpiryPolicy 한 곳에서 계산해, 목록(API 12)과 상세·추가·수정 응답(API 13·15·16)이 어긋나지 않게 한다. */
	public static FridgeIngredient of(Ingredient ingredient, IngredientCategory category, LocalDate today) {
		long dDay = ExpiryPolicy.dDay(ingredient.expirationDate(), today);
		return new FridgeIngredient(ingredient, category, dDay, ExpiryPolicy.statusOf(dDay));
	}
}
