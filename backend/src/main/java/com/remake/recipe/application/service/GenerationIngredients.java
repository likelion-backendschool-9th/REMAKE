package com.remake.recipe.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.application.port.out.RecipeAiRequest.FridgeItem;

/**
 * 생성에 쓸 냉장고 재료를 고르고, 재료 보충이 꺼져 있을 때 재료가 충분한지 판단한다.
 * <ul>
 * <li>소비기한이 지난 재료는 생성에 쓰지 않는다</li>
 * <li>커스텀에서 재료를 골랐으면 고른 재료만, 아니면 냉장고 전체를 쓴다. 내 진행 중 재료가 아니면 INGREDIENT_NOT_FOUND</li>
 * <li>재료 보충 OFF: 양념(SEASONING)을 뺀 재료가 {@link #MIN_MAIN_INGREDIENTS}개 미만이면 NO_INGREDIENTS(AI를 부르지 않음, 차감 없음)</li>
 * </ul>
 * 기준 2개는 레시피 영역 담당이 정한 값이다(정책 문서의 "너무 적으면"에 숫자가 없음).
 */
final class GenerationIngredients {

	/** 재료 보충 OFF일 때 필요한 양념 밖 재료 수 */
	static final int MIN_MAIN_INGREDIENTS = 2;

	private GenerationIngredients() {
	}

	/** 냉장고 목록(진행 중 재료)에서 생성에 쓸 재료를 고른다. 순서는 받은 순서(소비기한 빠른 순)를 지킨다. */
	static List<FridgeIngredient> select(List<FridgeIngredient> fridge, List<Long> chosenIds) {
		if (chosenIds.isEmpty()) {
			return fridge.stream().filter(GenerationIngredients::usable).toList();
		}
		Map<Long, FridgeIngredient> byId = fridge.stream()
				.collect(Collectors.toMap(f -> f.ingredient().id(), Function.identity()));
		Set<Long> chosen = Set.copyOf(chosenIds);
		if (!byId.keySet().containsAll(chosen)) {
			throw new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND);
		}
		return fridge.stream()
				.filter(f -> chosen.contains(f.ingredient().id()))
				.filter(GenerationIngredients::usable)
				.toList();
	}

	/**
	 * 다시 받기용. 원래 고른 재료 중 아직 냉장고에 있는 것만 쓴다(그 사이 다 쓰거나 지운 재료는 조용히 뺀다).
	 * 원래 고른 재료가 없었으면 냉장고 전체를 쓴다.
	 */
	static List<FridgeIngredient> selectRemaining(List<FridgeIngredient> fridge, List<Long> chosenIds) {
		if (chosenIds.isEmpty()) {
			return select(fridge, chosenIds);
		}
		Set<Long> chosen = Set.copyOf(chosenIds);
		return fridge.stream()
				.filter(f -> chosen.contains(f.ingredient().id()))
				.filter(GenerationIngredients::usable)
				.toList();
	}

	/** 재료 보충 OFF일 때 재료가 충분한지. 양념은 세지 않는다. */
	static boolean enoughWithoutSupplement(List<FridgeIngredient> selected) {
		long main = selected.stream()
				.filter(f -> f.category().code() != IngredientCategoryCode.SEASONING)
				.count();
		return main >= MIN_MAIN_INGREDIENTS;
	}

	static List<FridgeItem> toAiItems(List<FridgeIngredient> selected) {
		return selected.stream()
				.map(f -> new FridgeItem(f.ingredient().id(), f.ingredient().name(), f.ingredient().amount(),
						f.ingredient().unit().name(), f.ingredient().expirationDate(), f.status() != ExpiryStatus.OK))
				.toList();
	}

	private static boolean usable(FridgeIngredient f) {
		return f.dDay() >= 0;
	}
}
