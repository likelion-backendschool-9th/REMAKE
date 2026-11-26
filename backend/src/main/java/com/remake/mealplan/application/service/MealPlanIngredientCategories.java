package com.remake.mealplan.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.SlotIngredient;

import lombok.RequiredArgsConstructor;

/**
 * AI가 만든 칸 재료의 카테고리를 정한다(#91, 장바구니 담기와 레시피 재료 저장용).
 * 레시피 생성(#76, GeneratedRecipeValidator)과 같은 순서다: 사전 이름과 정확히 같으면 사전 카테고리 →
 * 그 줄의 냉장고 재료 카테고리 → AI가 고른 값. 셋 다 없으면 비워 두고, 그 재료는 장바구니에 담지 않는다(MealPlanCartItems).
 */
@Component
@RequiredArgsConstructor
class MealPlanIngredientCategories {

	private final GetIngredientCatalogUseCase getIngredientCatalogUseCase;
	private final GetIngredientCategoriesUseCase getIngredientCategoriesUseCase;

	/** 칸들의 재료 줄마다 카테고리를 다시 정한다. 같은 이름은 사전을 한 번만 찾는다. */
	List<GeneratedMealSlot> resolve(List<GeneratedMealSlot> slots, List<FridgeStock> fridge) {
		Map<Long, IngredientCategoryCode> fridgeCategories = new HashMap<>();
		fridge.stream().filter(stock -> stock.categoryCode() != null)
				.forEach(stock -> fridgeCategories.put(stock.ingredientId(), stock.categoryCode()));
		Map<String, Optional<IngredientCategoryCode>> catalog = new HashMap<>();
		return slots.stream()
				.map(slot -> new GeneratedMealSlot(slot.date(), slot.mealType(), slot.title(), slot.cookMinutes(),
						slot.tips(),
						slot.ingredients().stream()
								.map(ingredient -> withCategory(ingredient, fridgeCategories, catalog))
								.toList(),
						slot.steps(), slot.skipped()))
				.toList();
	}

	private SlotIngredient withCategory(SlotIngredient ingredient, Map<Long, IngredientCategoryCode> fridgeCategories,
			Map<String, Optional<IngredientCategoryCode>> catalog) {
		IngredientCategoryCode category = catalog.computeIfAbsent(ingredient.name().strip(), this::catalogCategory)
				.or(() -> Optional.ofNullable(ingredient.usesFridge()
						? fridgeCategories.get(ingredient.fridgeIngredientId())
						: null))
				.or(() -> Optional.ofNullable(ingredient.categoryCode()))
				.orElse(null);
		return ingredient.withCategoryCode(category);
	}

	private Optional<IngredientCategoryCode> catalogCategory(String name) {
		return getIngredientCatalogUseCase.findCatalogItemByName(name)
				.map(item -> getIngredientCategoriesUseCase.getIngredientCategory(item.categoryId()).code());
	}
}
