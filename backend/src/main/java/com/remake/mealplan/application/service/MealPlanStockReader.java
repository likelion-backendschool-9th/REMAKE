package com.remake.mealplan.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.AllocatedSlot;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.mealplan.domain.StockLedger;
import com.remake.recipe.application.port.in.GetRecipeIngredientsUseCase;
import com.remake.recipe.application.port.in.RecipeIngredientLine;

import lombok.RequiredArgsConstructor;

/**
 * 식단표 AI에 보낼 냉장고 재료 계산. 식단표 생성 작업(API 38)과 식단 칸 교체(API 42)가 같은 기준을 쓴다.
 * 냉장고 양에서 남는 칸들(요리 안 한 칸)이 쓰기로 한 양을 뺀다(StockLedger).
 */
@Component
@RequiredArgsConstructor
class MealPlanStockReader {

	private final GetFridgeUseCase getFridgeUseCase;
	private final GetRecipeIngredientsUseCase getRecipeIngredientsUseCase;

	/** 냉장고 재료(ACTIVE). 양·소비기한이 없는 재료는 계산할 수 없어 뺀다. */
	List<FridgeStock> loadFridge(Long userId) {
		return getFridgeUseCase.getFridgeIngredients(userId, null, null, null).stream()
				.map(FridgeIngredient::ingredient)
				.filter(ingredient -> ingredient.amount() != null && ingredient.expirationDate() != null)
				.map(MealPlanStockReader::toFridgeStock)
				.toList();
	}

	/**
	 * 냉장고 재료에서 남는 칸들의 배정량을 뺀 장부.
	 *
	 * @param remainingSlots 이번 생성·교체 뒤에도 남는 오늘 이후의 칸(덮어써서 지울 칸, 교체할 칸은 뺀다). 요리한 칸은 여기서 거른다
	 */
	StockLedger ledger(Long userId, List<FridgeStock> fridge, List<MealPlanSlot> remainingSlots, LocalDate today) {
		return StockLedger.of(fridge, allocatedSlots(userId, remainingSlots), today);
	}

	/** 레시피가 쓰는 냉장고 재료 id(재료 줄 순서, 중복 없음). 냉장고에 없는 재료 줄은 뺀다. */
	List<Long> fridgeIngredientIds(Long userId, Long recipeId) {
		return getRecipeIngredientsUseCase.getIngredients(userId, List.of(recipeId)).getOrDefault(recipeId, List.of())
				.stream()
				.map(RecipeIngredientLine::ingredientId)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	/** 요리하지 않은 칸들이 쓰기로 한 재료(그 칸 레시피의 재료 줄). */
	private List<AllocatedSlot> allocatedSlots(Long userId, List<MealPlanSlot> futureSlots) {
		List<MealPlanSlot> uncooked = futureSlots.stream().filter(slot -> !slot.isCooked()).toList();
		Map<Long, List<RecipeIngredientLine>> ingredients = getRecipeIngredientsUseCase.getIngredients(userId,
				uncooked.stream().map(MealPlanSlot::recipeId).distinct().toList());
		return uncooked.stream()
				.map(slot -> new AllocatedSlot(slot.slotDate(),
						ingredients.getOrDefault(slot.recipeId(), List.of()).stream()
								.map(MealPlanStockReader::toSlotIngredient)
								.toList()))
				.toList();
	}

	/** 내 냉장고에 없는 재료 id는 지운다(냉장고에 없는 재료로 본다). 다른 회원의 재료 id가 레시피에 저장되지 않게 한다. */
	static GeneratedMealSlot keepOwnFridgeIds(GeneratedMealSlot slot, List<FridgeStock> fridge) {
		Set<Long> ownIds = fridge.stream().map(FridgeStock::ingredientId).collect(Collectors.toSet());
		List<SlotIngredient> ingredients = slot.ingredients().stream()
				.map(ingredient -> ingredient.usesFridge() && !ownIds.contains(ingredient.fridgeIngredientId())
						? new SlotIngredient(null, ingredient.name(), ingredient.amount(), ingredient.unit())
						: ingredient)
				.toList();
		return new GeneratedMealSlot(slot.date(), slot.mealType(), slot.title(), slot.cookMinutes(), slot.tips(),
				ingredients, slot.steps(), slot.skipped());
	}

	private static FridgeStock toFridgeStock(Ingredient ingredient) {
		return new FridgeStock(ingredient.id(), ingredient.name(), ingredient.amount(), ingredient.unit(),
				ingredient.expirationDate());
	}

	private static SlotIngredient toSlotIngredient(RecipeIngredientLine line) {
		return new SlotIngredient(line.ingredientId(), line.name(), line.amount(), line.unit());
	}
}
