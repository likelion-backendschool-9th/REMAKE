package com.remake.ingredient.application.port.in;

import java.math.BigDecimal;

import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.IngredientLogType;

/**
 * 재료 사용·폐기 요청. via는 쓴 경로이고, COOK(요리 후 차감, #77)은 USED만 허용한다.
 */
public record ConsumeIngredientCommand(Long userId, Long ingredientId, IngredientLogType type, BigDecimal amount,
		ConsumeVia via) {
}
