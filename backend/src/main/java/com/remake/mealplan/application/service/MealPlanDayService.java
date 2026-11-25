package com.remake.mealplan.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.mealplan.application.port.in.GetMealPlanDayUseCase;
import com.remake.mealplan.application.port.in.MealPlanDay;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.recipe.application.port.in.GetRecipeListItemsUseCase;
import com.remake.recipe.application.port.in.RecipeListItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 날짜별 끼니(API 40). 칸은 식단표 저장소에서, 제목·재료 요약·즐겨찾기는 레시피 영역 UseCase로 한 번에 읽는다.
 * 칸 레시피는 생성·교체 때 이 회원 것으로 만들어 지울 수 없으므로 항상 읽힌다. 읽히지 않는 칸은 데이터 이상이라
 * 경고 로그를 남기고 빼서, 나머지 끼니는 보이게 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MealPlanDayService implements GetMealPlanDayUseCase {

	private final LoadMealPlanSlotPort loadMealPlanSlotPort;
	private final GetRecipeListItemsUseCase getRecipeListItemsUseCase;

	@Override
	@Transactional(readOnly = true)
	public MealPlanDay getDay(Long userId, LocalDate date) {
		List<MealPlanSlot> slots = loadMealPlanSlotPort.loadUserSlotsBetween(userId, date, date);
		Map<Long, RecipeListItem> recipes = getRecipeListItemsUseCase.getListItems(userId,
				slots.stream().map(MealPlanSlot::recipeId).distinct().toList());

		List<MealPlanDay.Slot> result = new ArrayList<>(slots.size());
		for (MealPlanSlot slot : slots) {
			RecipeListItem recipe = recipes.get(slot.recipeId());
			if (recipe == null) {
				log.warn("식단 칸의 레시피를 읽을 수 없어 하루 조회에서 뺍니다: slotId={}, recipeId={}", slot.id(),
						slot.recipeId());
				continue;
			}
			result.add(new MealPlanDay.Slot(slot.id(), slot.mealType(), recipe.recipeId(), recipe.title(),
					recipe.ingredientSummary(), recipe.extraIngredientCount(), recipe.favorite(), slot.isCooked()));
		}
		return new MealPlanDay(date, result);
	}
}
