package com.remake.mealplan.application.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.LoadMealPlanCombinationRulesPort;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPlanCombinationRules;
import com.remake.mealplan.domain.SlotIngredient;

import lombok.extern.slf4j.Slf4j;

/**
 * 식단표 생성(38)·칸 교체(42)의 재료 조합 기준(#288 F, 중간 발표 "엉터리 조합" 지적). 규칙은 MealPlanCombinationRules.
 * <ul>
 * <li>F1: AI에 보낼 냉장고 재료에서 식약처 요리에 안 나오는 재료(콜라·초콜릿·과자 등)와 사전에 없는 음료를 뺀다</li>
 * <li>F2: AI가 만든 칸에 처음 보는 주의 재료 짝이 있으면 AI_INVALID_RESPONSE로 실패시킨다. 생성·교체가 1회 다시 부르고,
 * 또 걸리면 다른 형식 오류처럼 실패한다(차감 없음). "못 만듦" 칸은 검사하지 않는다</li>
 * <li>이름·재료 검사: 요리 이름에 든 재료가 재료 목록에 없는 칸 수를 로그로만 남긴다(막지 않음, 11-26 결정).
 * 프롬프트는 쓰지 않은 재료를 이름에서 빼라고 지시한다</li>
 * </ul>
 * 로그에는 걸린 칸 수·뺀 재료 수만 남기고 재료 이름은 남기지 않는다.
 */
@Slf4j
@Component
class MealPlanCombinationCheck {

	private final MealPlanCombinationRules rules;

	MealPlanCombinationCheck(LoadMealPlanCombinationRulesPort loadMealPlanCombinationRulesPort) {
		this.rules = loadMealPlanCombinationRulesPort.load();
	}

	/** F1: AI에 보낼 냉장고 재료. 순서는 그대로다. */
	List<FridgeStock> forAi(List<FridgeStock> available) {
		List<FridgeStock> result = available.stream()
				.filter(stock -> !rules.excludedFromAi(stock.name(), stock.categoryCode()))
				.toList();
		if (result.size() < available.size()) {
			log.info("식단표 AI에 보내지 않은 냉장고 재료 {}개", available.size() - result.size());
		}
		return result;
	}

	/** F2: 검사를 통과하면 그대로 돌려준다. 이름·재료가 안 맞는 칸 수도 함께 로그로 남긴다. */
	List<GeneratedMealSlot> check(List<GeneratedMealSlot> slots) {
		long titleMismatchSlots = slots.stream()
				.filter(slot -> !slot.skipped())
				.filter(slot -> !rules.missingTitleIngredients(slot.title(),
						slot.ingredients().stream().map(SlotIngredient::name).toList()).isEmpty())
				.count();
		if (titleMismatchSlots > 0) {
			log.info("식단표 AI 응답 이름에 재료 목록에 없는 재료가 든 칸: {}칸", titleMismatchSlots);
		}
		long oddSlots = slots.stream()
				.filter(slot -> !slot.skipped())
				.filter(slot -> rules.unseenCautionPairs(
						slot.ingredients().stream().map(SlotIngredient::name).toList()) > 0)
				.count();
		if (oddSlots > 0) {
			log.warn("식단표 AI 응답 조합 검사 실패: {}칸", oddSlots);
			throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
		}
		return slots;
	}
}
