package com.remake.recipe.application.port.in;

import java.math.BigDecimal;

import com.remake.recipe.domain.RecipeUnit;

/**
 * 레시피 재료 한 줄(다른 기능이 읽는 모양).
 *
 * @param ingredientId 생성할 때 쓴 내 냉장고 재료 id. 냉장고에 없는 재료이거나 재료를 지웠으면 null
 * @param amount       양. null이면 "약간"
 */
public record RecipeIngredientLine(Long ingredientId, String name, BigDecimal amount, RecipeUnit unit) {
}
