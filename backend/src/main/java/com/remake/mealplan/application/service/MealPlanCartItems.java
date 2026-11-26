package com.remake.mealplan.application.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.remake.cart.application.port.in.CartAdditionCommand;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.SlotIngredient;

import lombok.extern.slf4j.Slf4j;

/**
 * 칸 레시피의 부족 재료를 장바구니 담기 명령으로 바꾼다(#91). 식단표 생성(API 38)과 칸 교체(API 42)가 같은 기준을 쓴다.
 * <ul>
 * <li>부족 재료 = 냉장고에 없는 재료 중 양을 셀 수 있고 물이 아닌 것(SlotIngredient.isShortage, 일수 자르기와 같은 기준)</li>
 * <li>카테고리를 정하지 못한 재료와 장바구니 이름 길이(50자)를 넘는 재료는 담지 않는다. 담기가 INVALID_INPUT으로
 * 거절해 식단표 저장 전체가 실패하지 않게 하려는 것이다(레시피 저장 #76과 같은 처리)</li>
 * <li>그 칸 날짜까지 쓸 수 있는 같은 이름의 냉장고 재료가 있으면 담지 않는다. 단위가 달라 냉장고 재료로 다시 연결하지 못한 줄이다
 * (MealPlanStockReader.relinkFridgeByName, 11-26 실측 "대파 0.5PIECE" vs 냉장고 2BUNCH). 레시피 저장(#76)도 이름으로만 본다</li>
 * <li>원본 recipeId와 그 칸 id를 넣는다. 칸 id는 처음 담은 출처로만 남는다(#57)</li>
 * </ul>
 */
@Slf4j
final class MealPlanCartItems {

	/** cart_items.name 길이(장바구니 담기가 50자를 넘는 이름을 거절한다) */
	static final int CART_NAME_MAX_LENGTH = 50;

	private MealPlanCartItems() {
	}

	static List<CartAdditionCommand> shortages(GeneratedMealSlot slot, Long recipeId, Long slotId,
			List<FridgeStock> fridge) {
		Set<String> owned = fridge.stream()
				.filter(stock -> !stock.expirationDate().isBefore(slot.date()))
				.map(stock -> stock.name().strip())
				.collect(Collectors.toSet());
		List<SlotIngredient> shortages = slot.ingredients().stream()
				.filter(SlotIngredient::isShortage)
				.filter(ingredient -> !owned.contains(ingredient.name().strip()))
				.toList();
		List<CartAdditionCommand> commands = shortages.stream()
				.filter(ingredient -> ingredient.categoryCode() != null)
				.filter(ingredient -> ingredient.name().strip().length() <= CART_NAME_MAX_LENGTH)
				.map(ingredient -> new CartAdditionCommand(recipeId, slotId, ingredient.name(), ingredient.amount(),
						ingredient.unit(), ingredient.categoryCode()))
				.toList();
		if (commands.size() < shortages.size()) {
			// 재료 이름은 남기지 않는다(개수만)
			log.info("장바구니에 담지 못한 부족 재료 slotId={} count={}", slotId, shortages.size() - commands.size());
		}
		return commands;
	}
}
