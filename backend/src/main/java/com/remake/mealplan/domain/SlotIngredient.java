package com.remake.mealplan.domain;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.domain.RecipeUnit;

/**
 * 칸 레시피의 재료 한 줄(AI가 만든 것 또는 저장된 레시피의 것).
 *
 * @param fridgeIngredientId 이 재료로 쓰는 내 냉장고 재료 id. 냉장고에 없는 재료면 null
 * @param amount             양. null이면 "약간"
 * @param categoryCode       재료 카테고리(#91, 장바구니 담기용). AI 응답에서는 AI가 고른 값이고,
 *                           저장 전에 사전 → 냉장고 재료 → AI 순서로 다시 정한다(MealPlanIngredientCategories). 모르면 null
 */
public record SlotIngredient(Long fridgeIngredientId, String name, BigDecimal amount, RecipeUnit unit,
		IngredientCategoryCode categoryCode) {

	/**
	 * 냉장고에 없어도 부족 재료로 세지 않는(장바구니에 담지 않는) 유일한 재료. 물은 냉장고에 등록하는 품목이 아니다.
	 * 레시피 생성의 GeneratedRecipeValidator.WATER(레시피 영역 결정, 가이드 04)와 같게 둔다.
	 * 소금·식용유 같은 양념은 예외가 아니다(#158).
	 */
	static final String WATER = "물";

	/** 카테고리를 모르는 줄(저장된 레시피의 재료로 배정량을 계산할 때 등). */
	public SlotIngredient(Long fridgeIngredientId, String name, BigDecimal amount, RecipeUnit unit) {
		this(fridgeIngredientId, name, amount, unit, null);
	}

	public SlotIngredient {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("재료 이름이 필요합니다.");
		}
		if (unit == null) {
			throw new IllegalArgumentException("재료 단위가 필요합니다.");
		}
		if (amount != null && amount.signum() <= 0) {
			throw new IllegalArgumentException("재료 양은 0보다 커야 합니다.");
		}
	}

	/** 양을 셀 수 있는 재료인지. 양이 없거나 꼬집·약간이면 배정·장바구니 계산에서 뺀다. */
	public boolean isMeasured() {
		return amount != null && unit != RecipeUnit.PINCH && unit != RecipeUnit.TO_TASTE;
	}

	/** 냉장고 재료와 이어진 재료인지. */
	public boolean usesFridge() {
		return fridgeIngredientId != null;
	}

	/** 물인지(앞뒤 공백은 무시하고 이름이 정확히 "물"). 물은 부족 재료로 세지 않는다. */
	public boolean isWater() {
		return WATER.equals(name.strip());
	}

	/** 부족 재료(장바구니 대상)인지: 냉장고에 없는 재료이고, 양을 셀 수 있고, 물이 아니다. */
	public boolean isShortage() {
		return !usesFridge() && isMeasured() && !isWater();
	}

	/** 같은 줄에 냉장고 재료 연결만 지운다(내 냉장고에 없는 id). 카테고리는 그대로 둔다. */
	public SlotIngredient withoutFridge() {
		return new SlotIngredient(null, name, amount, unit, categoryCode);
	}

	/** 같은 줄을 내 냉장고 재료에 연결한다(이름이 같은 냉장고 재료로 다시 연결할 때). */
	public SlotIngredient withFridge(Long id) {
		return new SlotIngredient(id, name, amount, unit, categoryCode);
	}

	/** 같은 줄에 카테고리만 바꾼다. */
	public SlotIngredient withCategoryCode(IngredientCategoryCode code) {
		return new SlotIngredient(fridgeIngredientId, name, amount, unit, code);
	}
}
