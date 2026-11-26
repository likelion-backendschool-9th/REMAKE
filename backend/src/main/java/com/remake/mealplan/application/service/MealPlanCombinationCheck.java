package com.remake.mealplan.application.service;

import java.util.ArrayList;
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
 * <li>이름·재료 검사: 요리 이름에 든 재료가 재료 목록에 없으면 서버가 이름을 고친다(막지 않음, 11-26 결정,
 * MealPlanCombinationRules.fixedTitle). 프롬프트도 쓰지 않은 재료를 이름에서 빼라고 지시하고, 이건 마지막 안전장치다</li>
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

	/** F2 검사를 통과하면, 이름·재료가 안 맞는 칸의 이름을 고친 칸들을 돌려준다(순서 그대로). */
	List<GeneratedMealSlot> check(List<GeneratedMealSlot> slots) {
		long oddSlots = slots.stream()
				.filter(slot -> !slot.skipped())
				.filter(slot -> rules.unseenCautionPairs(
						slot.ingredients().stream().map(SlotIngredient::name).toList()) > 0)
				.count();
		if (oddSlots > 0) {
			log.warn("식단표 AI 응답 조합 검사 실패: {}칸", oddSlots);
			throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
		}
		return fixTitles(slots);
	}

	private List<GeneratedMealSlot> fixTitles(List<GeneratedMealSlot> slots) {
		int fixedSlots = 0;
		List<GeneratedMealSlot> result = new ArrayList<>(slots.size());
		for (GeneratedMealSlot slot : slots) {
			if (slot.skipped()) {
				result.add(slot);
				continue;
			}
			String fixed = rules.fixedTitle(slot.title(), slot.ingredients().stream().map(SlotIngredient::name).toList());
			// 이름 길이 검사(GeneratedMealPlanValidator)는 고치기 전에 하므로, 주재료를 붙여 한도를 넘으면 원래 이름을 둔다
			if (fixed.equals(slot.title()) || fixed.strip().length() > GeneratedMealPlanValidator.TITLE_MAX_LENGTH) {
				result.add(slot);
			} else {
				result.add(slot.withTitle(fixed));
				fixedSlots++;
			}
		}
		if (fixedSlots > 0) {
			log.info("식단표 AI 응답 이름에 없는 재료가 있어 이름을 고친 칸: {}칸", fixedSlots);
		}
		return result;
	}
}
