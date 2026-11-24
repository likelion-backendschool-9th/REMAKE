package com.remake.recipe.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.recipe.application.port.out.GeneratedRecipe;
import com.remake.recipe.application.port.out.RecipeAiRequest;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeUnit;

/**
 * AI 응답을 검증해 저장할 Recipe로 바꾼다. 통과하지 못하면 AI_INVALID_RESPONSE를 던진다(Service가 1회 재시도).
 * <ul>
 * <li>필수 값(이름·조리 시간·재료·단계)이 없거나 Recipe 규칙을 어기면 실패</li>
 * <li>14개 단위 코드에 없는 단위의 재료는 그 줄만 뺀다. 남은 재료가 없으면 실패</li>
 * <li>요청에 없는 냉장고 재료 id는 냉장고에 없는 재료(null)로 본다</li>
 * <li>재료 보충이 꺼져 있으면 냉장고에 없는 재료는 실패. 물({@link #WATER})만 예외이고, 소금·식용유 같은 양념과 꼬집·약간은 예외가 아니다(#158)</li>
 * <li>인분은 AI 값이 아니라 요청한 인분을 쓴다. "약간"(TO_TASTE)은 양을 비우고, 양은 소수 셋째 자리로 반올림한다</li>
 * </ul>
 */
final class GeneratedRecipeValidator {

	/** 재료 보충이 꺼져 있어도 냉장고에 없이 쓸 수 있는 유일한 재료. 물은 냉장고에 등록하는 품목이 아니다(레시피 영역 담당 결정) */
	static final String WATER = "물";

	private static final int AMOUNT_SCALE = 3;

	private GeneratedRecipeValidator() {
	}

	static Recipe toRecipe(Long userId, RecipeAiRequest request, GeneratedRecipe generated) {
		if (generated == null || generated.cookMinutes() == null || generated.ingredients() == null) {
			throw invalid();
		}
		Set<Long> fridgeIds = request.fridgeIngredients().stream()
				.map(RecipeAiRequest.FridgeItem::ingredientId)
				.collect(Collectors.toSet());

		List<RecipeIngredient> ingredients = new ArrayList<>();
		for (GeneratedRecipe.Ingredient item : generated.ingredients()) {
			if (item == null) {
				continue;
			}
			RecipeUnit unit = parseUnit(item.unit());
			if (unit == null) {
				continue;
			}
			Long ingredientId = fridgeIds.contains(item.fridgeIngredientId()) ? item.fridgeIngredientId() : null;
			if (ingredientId == null && !request.supplementEnabled() && !isWater(item.name())) {
				throw invalid();
			}
			ingredients.add(toIngredient(ingredientId, item.name(), item.amount(), unit));
		}

		try {
			return Recipe.create(userId, request.source(), generated.title(), request.servings(),
					generated.cookMinutes(), generated.tips(), request.customOptions(), ingredients,
					generated.steps());
		} catch (IllegalArgumentException e) {
			throw invalid();
		}
	}

	private static RecipeIngredient toIngredient(Long ingredientId, String name, BigDecimal amount, RecipeUnit unit) {
		BigDecimal normalized = unit == RecipeUnit.TO_TASTE || amount == null ? null : normalizeAmount(amount);
		try {
			return new RecipeIngredient(ingredientId, name, normalized, unit);
		} catch (IllegalArgumentException e) {
			throw invalid();
		}
	}

	/** 소수 셋째 자리로 반올림하고 끝의 0을 지운다. 300이 3E+2로 바뀌지 않게 정수는 소수 자리 0으로 둔다. */
	private static BigDecimal normalizeAmount(BigDecimal amount) {
		BigDecimal stripped = amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP).stripTrailingZeros();
		return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
	}

	/** 물인지. 물은 냉장고에 없어도 부족한 재료로 세지 않는다(장바구니에도 담지 않는다) */
	static boolean isWater(String name) {
		return name != null && WATER.equals(name.strip());
	}

	/** 앞뒤 공백·대소문자는 맞춰 주고, 14개 코드에 없으면 null */
	private static RecipeUnit parseUnit(String unit) {
		if (unit == null || unit.isBlank()) {
			return null;
		}
		try {
			return RecipeUnit.valueOf(unit.strip().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
	}
}
