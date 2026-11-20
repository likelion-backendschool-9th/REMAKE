package com.remake.ingredient.domain;

/** 사용자별 만료 재료 개수(알림용). 만료 알림은 하루 한 건에 "N개"만 쓴다. */
public record ExpiredIngredientCount(Long userId, long count) {
}
