package com.remake.ingredient.domain;

/** 냉장고 목록 한 줄. 재료에 카테고리와 오늘 기준 D-day·상태를 붙였다. dDay가 음수면 기한이 지난 것이다. */
public record FridgeIngredient(Ingredient ingredient, IngredientCategory category, long dDay, ExpiryStatus status) {
}
