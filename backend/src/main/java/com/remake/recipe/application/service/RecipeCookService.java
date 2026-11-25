package com.remake.recipe.application.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.application.port.in.ConsumeIngredientUseCase;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.mealplan.application.port.in.MarkMealSlotCookedUseCase;
import com.remake.recipe.application.port.in.CookRecipeUseCase;
import com.remake.recipe.application.port.in.GetRecipeUseCase;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.recipe.domain.UnitConversion;

import lombok.RequiredArgsConstructor;

/**
 * 요리 후 재료 차감 미리보기(API 31)와 요리 완료(API 32).
 * 차감·기록·E25는 냉장고 영역의 ConsumeIngredientUseCase가 하고(이 트랜잭션에 합류), 여기서는 E49 cook_completed만 남긴다.
 */
@Service
@RequiredArgsConstructor
class RecipeCookService implements CookRecipeUseCase {

	private final GetRecipeUseCase getRecipeUseCase;
	private final GetFridgeUseCase getFridgeUseCase;
	private final ConsumeIngredientUseCase consumeIngredientUseCase;
	private final MarkMealSlotCookedUseCase markMealSlotCookedUseCase;
	private final EventTracker eventTracker;

	@Override
	@Transactional(readOnly = true)
	public List<PreviewItem> preview(Long userId, Long recipeId) {
		return previewOf(userId, getRecipeUseCase.getRecipe(userId, recipeId).recipe());
	}

	@Override
	@Transactional
	public int cook(Long userId, Long recipeId, List<CookItem> items, Long mealPlanSlotId) {
		if (items == null || items.stream().anyMatch(i -> i == null || i.ingredientId() == null || i.amount() == null)) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		Recipe recipe = getRecipeUseCase.getRecipe(userId, recipeId).recipe();
		Set<Long> ids = new HashSet<>();
		for (CookItem item : items) {
			if (item.amount().signum() <= 0 || !ids.add(item.ingredientId())) {
				throw new BusinessException(ErrorCode.COOK_ITEM_INVALID);
			}
		}
		// 내 냉장고에 지금 있는 재료인지 먼저 본다(다른 사용자의 재료, 이미 다 써서 지운 재료). 확인 뒤 다른 요청이 먼저 다 쓰면
		// 차감 때 INGREDIENT_NOT_FOUND가 난다(errors.md "Cook deduction pressed twice")
		Set<Long> mine = getFridgeUseCase.getFridgeIngredients(userId, null, null, null).stream()
				.map(f -> f.ingredient().id())
				.collect(Collectors.toSet());
		if (!mine.containsAll(ids)) {
			throw new BusinessException(ErrorCode.COOK_ITEM_INVALID);
		}

		// adjusted_count: 미리보기가 정한 양과 다르게 차감한 재료 수(미리보기가 양을 정하지 못한 재료 포함)
		Map<Long, BigDecimal> suggested = previewOf(userId, recipe).stream()
				.filter(p -> p.amount() != null)
				.collect(Collectors.toMap(PreviewItem::ingredientId, PreviewItem::amount));
		long adjusted = items.stream()
				.filter(i -> !suggested.containsKey(i.ingredientId())
						|| suggested.get(i.ingredientId()).compareTo(i.amount()) != 0)
				.count();

		// 식단 칸을 재료보다 먼저 잠근다. 식단표 덮어쓰기 저장은 칸을 잠근 뒤 새 레시피 재료로 냉장고 재료 행을 참조하므로,
		// 재료를 먼저 잠그면 두 트랜잭션이 서로를 기다릴 수 있다(교착)
		if (mealPlanSlotId != null) {
			markMealSlotCookedUseCase.markCooked(userId, mealPlanSlotId, recipe.id());
		}
		// 여러 요청이 같은 재료를 잠글 때 순서가 엇갈리지 않게 id 순으로 뺀다
		items.stream().sorted(Comparator.comparing(CookItem::ingredientId)).forEach(item ->
				consumeIngredientUseCase.consumeIngredient(new ConsumeIngredientCommand(userId, item.ingredientId(),
						IngredientLogType.USED, item.amount(), ConsumeVia.COOK)));
		eventTracker.track(EventName.COOK_COMPLETED, Screen.COOK_DEDUCT, Map.of(
				"recipe_id", recipe.id(),
				"deducted_count", items.size(),
				"adjusted_count", adjusted,
				"from_meal_plan", mealPlanSlotId != null), EventTrackCommand.ofUser(userId));
		return items.size();
	}

	/** 레시피 재료마다 냉장고 재료 하나와 짝짓는다. 같은 냉장고 재료에 두 줄이 걸리면 양을 더한다(하나라도 못 바꾸면 null). */
	private List<PreviewItem> previewOf(Long userId, Recipe recipe) {
		// 소비기한이 빠른 순(GetFridgeUseCase 정렬)이라 같은 이름이면 앞의 것을 고른다. 지난 재료는 없는 것으로 본다("바로 가능"과 같음)
		Map<String, FridgeIngredient> byName = getFridgeUseCase.getFridgeIngredients(userId, null, null, null)
				.stream()
				.filter(f -> f.dDay() >= 0)
				.collect(Collectors.toMap(f -> f.ingredient().name().strip(), Function.identity(), (first, later) -> first,
						LinkedHashMap::new));
		Map<Long, PreviewItem> items = new LinkedHashMap<>();
		for (RecipeIngredient line : recipe.ingredients()) {
			if (!deductible(line)) {
				continue;
			}
			FridgeIngredient match = byName.get(line.name().strip());
			if (match == null) {
				continue;
			}
			Ingredient ingredient = match.ingredient();
			BigDecimal amount = UnitConversion.convert(line.amount(), line.unit(), ingredient.unit()).orElse(null);
			PreviewItem item = new PreviewItem(ingredient.id(), ingredient.name(), match.category().imageUrl(), amount,
					ingredient.unit(), ingredient.amount());
			items.merge(ingredient.id(), item, RecipeCookService::sum);
		}
		return items.values().stream().map(RecipeCookService::capped).toList();
	}

	/** 물(#158)·꼬집·약간·양 없는 재료는 차감하지 않는다(domain-rules "Units"). */
	private static boolean deductible(RecipeIngredient line) {
		return line.amount() != null && line.unit() != RecipeUnit.PINCH && line.unit() != RecipeUnit.TO_TASTE
				&& !GeneratedRecipeValidator.isWater(line.name());
	}

	private static PreviewItem sum(PreviewItem a, PreviewItem b) {
		BigDecimal amount = a.amount() == null || b.amount() == null ? null : a.amount().add(b.amount());
		return new PreviewItem(a.ingredientId(), a.name(), a.imageUrl(), amount, a.unit(), a.remainingAmount());
	}

	/** 남은 양을 넘지 않게 한다. 반올림으로 0이 되면(아주 적은 양) 사용자가 정하게 null로 둔다. */
	private static PreviewItem capped(PreviewItem item) {
		BigDecimal amount = item.amount();
		if (amount != null) {
			amount = amount.signum() == 0 ? null : amount.min(item.remainingAmount()).stripTrailingZeros();
		}
		return Objects.equals(amount, item.amount()) ? item
				: new PreviewItem(item.ingredientId(), item.name(), item.imageUrl(), amount, item.unit(),
						item.remainingAmount());
	}
}
