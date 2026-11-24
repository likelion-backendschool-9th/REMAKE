package com.remake.mealplan.application.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;

/**
 * AI가 만든 칸들을 저장 전에 검사한다. 칸 하나라도 망가지면 응답 전체를 AI_INVALID_RESPONSE로 실패시킨다
 * (서버 작업이 1회 재시도한다). 칸 하나만 버리면 그날부터 일수가 잘려 식단이 크게 짧아지기 때문이다(11-24 결정).
 * <ul>
 * <li>채울 칸이 모두 한 번씩 있어야 한다. 빠진 칸·같은 칸 두 번·요청하지 않은 칸(남기는 칸 포함)은 실패</li>
 * <li>레시피 저장 규칙(recipe Recipe·RecipeIngredient)과 같은 한도: 이름 100자, 조리 1분 이상, 재료·단계 1개 이상,
 * 단계·팁 1000자, 재료 이름 100자, 양 정수 7자리. 저장 때 처음 걸리면 INTERNAL_ERROR가 되므로 여기서 먼저 거른다</li>
 * </ul>
 * 없는 단위 줄 빼기·양 반올림·"약간" 양 비우기는 어댑터가 읽을 때 하고, 다른 회원의 냉장고 재료 id는 서버 작업이 지운다.
 * 목적 규칙(조리 시간·재료 수 등)은 검사하지 않는다(지시만 한다).
 */
final class GeneratedMealPlanValidator {

	static final int TITLE_MAX_LENGTH = 100;
	static final int TIPS_MAX_LENGTH = 1000;
	static final int STEP_MAX_LENGTH = 1000;
	static final int INGREDIENT_NAME_MAX_LENGTH = 100;
	static final int AMOUNT_MAX_INTEGER_DIGITS = 7;

	private GeneratedMealPlanValidator() {
	}

	static List<GeneratedMealSlot> validate(MealSchedule schedule, List<GeneratedMealSlot> generated) {
		if (generated == null) {
			throw invalid();
		}
		Set<MealSlotKey> requested = new HashSet<>();
		for (var date : schedule.dates()) {
			for (MealType mealType : schedule.mealTypesOn(date)) {
				requested.add(new MealSlotKey(date, mealType));
			}
		}
		Set<MealSlotKey> seen = new HashSet<>();
		for (GeneratedMealSlot slot : generated) {
			if (slot == null || !requested.contains(slot.key()) || !seen.add(slot.key())) {
				throw invalid();
			}
			checkRecipe(slot);
		}
		if (!seen.equals(requested)) {
			throw invalid();
		}
		return generated;
	}

	private static void checkRecipe(GeneratedMealSlot slot) {
		if (isBlank(slot.title()) || slot.title().strip().length() > TITLE_MAX_LENGTH) {
			throw invalid();
		}
		if (slot.cookMinutes() < 1) {
			throw invalid();
		}
		if (slot.tips() != null && slot.tips().strip().length() > TIPS_MAX_LENGTH) {
			throw invalid();
		}
		if (slot.ingredients().isEmpty() || slot.steps().isEmpty()) {
			throw invalid();
		}
		for (SlotIngredient ingredient : slot.ingredients()) {
			if (ingredient.name().strip().length() > INGREDIENT_NAME_MAX_LENGTH || tooLarge(ingredient.amount())) {
				throw invalid();
			}
		}
		for (String step : slot.steps()) {
			if (isBlank(step) || step.strip().length() > STEP_MAX_LENGTH) {
				throw invalid();
			}
		}
	}

	private static boolean tooLarge(BigDecimal amount) {
		if (amount == null) {
			return false;
		}
		BigDecimal stripped = amount.stripTrailingZeros();
		return stripped.precision() - stripped.scale() > AMOUNT_MAX_INTEGER_DIGITS;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
	}
}
