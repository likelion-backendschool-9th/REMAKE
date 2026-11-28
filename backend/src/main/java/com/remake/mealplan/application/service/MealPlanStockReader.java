package com.remake.mealplan.application.service;

import java.time.LocalDate;
import java.util.Comparator;
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
import com.remake.recipe.domain.UnitConversion;

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

	/** 냉장고 재료(ACTIVE)와 그 카테고리. 양·소비기한이 없는 재료는 계산할 수 없어 뺀다. */
	List<FridgeStock> loadFridge(Long userId) {
		return getFridgeUseCase.getFridgeIngredients(userId, null, null, null).stream()
				.filter(fridge -> fridge.ingredient().amount() != null && fridge.ingredient().expirationDate() != null)
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
						? ingredient.withoutFridge()
						: ingredient)
				.toList();
		return new GeneratedMealSlot(slot.date(), slot.mealType(), slot.title(), slot.cookMinutes(), slot.tips(),
				ingredients, slot.steps(), slot.skipped());
	}

	/**
	 * AI가 냉장고 재료를 냉장고 id 없이 "사야 할 재료"로 쓴 줄을 그 냉장고 재료로 다시 연결한다(#91, 11-26 실측: 계란이 넉넉한데
	 * "계란 2개"를 사야 할 재료로 씀). 연결하면 일수 자르기가 남은 양 안에서만 쓰게 하고, 모자라면 그날부터 자른다
	 * (#78: 냉장고 재료가 모자라면 장바구니에 넣지 않는다). 요리 완료 때 냉장고 차감도 된다.
	 * <ul>
	 * <li>양을 셀 수 있는 줄만(꼬집·약간은 배정·장바구니와 상관없다)</li>
	 * <li>이름이 같고(앞뒤 공백 무시), 단위를 그 냉장고 재료 단위로 바꿀 수 있고, 그 칸 날짜까지 쓸 수 있는 냉장고 재료</li>
	 * <li>여럿이면 소비기한이 가장 빠른 것(기한 임박 재료를 먼저 쓰는 규칙과 같다)</li>
	 * </ul>
	 * 단위를 바꿀 수 없으면(예: 대파 PIECE vs 냉장고 BUNCH) 연결하지 않는다. 그 줄은 장바구니에도 담지 않는다(MealPlanCartItems).
	 */
	static GeneratedMealSlot relinkFridgeByName(GeneratedMealSlot slot, List<FridgeStock> fridge) {
		List<SlotIngredient> ingredients = slot.ingredients().stream()
				.map(ingredient -> ingredient.usesFridge() || !ingredient.isMeasured()
						? ingredient
						: fridge.stream()
								.filter(stock -> stock.name().strip().equals(ingredient.name().strip()))
								.filter(stock -> !stock.expirationDate().isBefore(slot.date()))
								.filter(stock -> UnitConversion.convert(ingredient.amount(), ingredient.unit(),
										stock.unit()).isPresent())
								.min(Comparator.comparing(FridgeStock::expirationDate))
								.map(stock -> ingredient.withFridge(stock.ingredientId()))
								.orElse(ingredient))
				.toList();
		return new GeneratedMealSlot(slot.date(), slot.mealType(), slot.title(), slot.cookMinutes(), slot.tips(),
				ingredients, slot.steps(), slot.skipped());
	}

	/** AI 응답 칸 정리: 다른 회원 재료 id 지우기 → 이름이 같은 냉장고 재료로 다시 연결하기. */
	static GeneratedMealSlot normalizeFridgeLinks(GeneratedMealSlot slot, List<FridgeStock> fridge) {
		return relinkFridgeByName(keepOwnFridgeIds(slot, fridge), fridge);
	}

	private static FridgeStock toFridgeStock(FridgeIngredient fridge) {
		Ingredient ingredient = fridge.ingredient();
		return new FridgeStock(ingredient.id(), ingredient.name(), ingredient.amount(), ingredient.unit(),
				ingredient.expirationDate(), fridge.category() == null ? null : fridge.category().code());
	}

	private static SlotIngredient toSlotIngredient(RecipeIngredientLine line) {
		return new SlotIngredient(line.ingredientId(), line.name(), line.amount(), line.unit());
	}
}
