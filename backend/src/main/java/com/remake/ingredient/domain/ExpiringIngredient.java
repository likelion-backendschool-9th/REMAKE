package com.remake.ingredient.domain;

import java.time.LocalDate;

/** 임박 재료(알림용). 알림은 재료별로 만든다. */
public record ExpiringIngredient(Long userId, Long ingredientId, String name, LocalDate expirationDate) {
}
