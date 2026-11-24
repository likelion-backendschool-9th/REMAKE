package com.remake.cart.application.port.in;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;

/**
 * 호출하는 서버 기능이 확정한 부족 재료. 원본 recipeId는 모든 출처에서 필수이다.
 * categoryCode는 이름이 사전에 없을 때 사용할 서버 확정값이며 사전 값이 우선이다.
 * null 수량·PINCH·TO_TASTE·물은 담기에서 제외한다. 슬롯은 최초 담기 출처로만 저장한다.
 */
public record CartAdditionCommand(Long recipeId, Long mealPlanSlotId, String ingredientName,
		BigDecimal amount, RecipeUnit unit, IngredientCategoryCode categoryCode) { }
