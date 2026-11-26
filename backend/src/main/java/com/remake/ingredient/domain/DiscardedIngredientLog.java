package com.remake.ingredient.domain;

import java.time.LocalDateTime;

/**
 * 버린 기록 한 건(영수증 모듈의 버린 식비·식재료 조회용). 이름·카테고리·금액은 버릴 때 저장한 값이다. costAmount는 가격이 없으면 null.
 */
public record DiscardedIngredientLog(String name, IngredientCategoryCode categoryCode, LocalDateTime discardedAt,
		Long costAmount) {
}
