package com.remake.ingredient.domain;

/** 어제 만료된 재료(만료 이벤트용). 이벤트 속성 category_code, has_price를 만들 수 있게 필요한 값만 담는다. */
public record ExpiredIngredient(Long userId, Long ingredientId, IngredientCategoryCode categoryCode, boolean hasPrice) {
}
