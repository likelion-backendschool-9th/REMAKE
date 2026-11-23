package com.remake.ingredient.domain;

import java.math.BigDecimal;

/**
 * 재료를 쓰거나 버린 기록. 만든 뒤 고치지 않는다. name·unit은 그때의 재료 이름·단위이고(재료가 지워져도 남는다),
 * ingredientId는 재료가 지워지면 null이 된다. costAmount는 버린 재료의 가격 비례 금액이다(사용 기록이나 가격이 없는 재료는 null).
 */
public record IngredientLog(Long id, Long userId, Long ingredientId, IngredientLogType type, String name,
		BigDecimal amount, IngredientUnit unit, Long costAmount) {
}
