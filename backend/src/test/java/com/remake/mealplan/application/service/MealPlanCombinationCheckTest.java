package com.remake.mealplan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.MealPlanCombinationRules;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.recipe.domain.RecipeUnit;

class MealPlanCombinationCheckTest {

	private static final LocalDate DAY = LocalDate.of(2026, 11, 27);

	/** 딸기는 우유하고만 같이 쓰였다. */
	private static final MealPlanCombinationCheck CHECK = new MealPlanCombinationCheck(() -> {
		Map<String, IngredientCategoryCode> catalog = new LinkedHashMap<>();
		catalog.put("두부", IngredientCategoryCode.BEAN_TOFU);
		catalog.put("계란", IngredientCategoryCode.DAIRY_EGG);
		catalog.put("딸기", IngredientCategoryCode.FRUIT);
		catalog.put("우유", IngredientCategoryCode.DAIRY_EGG);
		catalog.put("감자", IngredientCategoryCode.VEGETABLE);
		catalog.put("방울토마토", IngredientCategoryCode.VEGETABLE);
		return new MealPlanCombinationRules(catalog, Map.of(), Map.of(), Map.of(), Set.of(), Set.of(), List.of(),
				Set.of(), Set.of(), Set.of(), Map.of("딸기", Set.of("우유")));
	});

	private static GeneratedMealSlot slot(String title, String... names) {
		return new GeneratedMealSlot(DAY, MealType.DINNER, title, 10, null,
				List.of(names).stream().map(n -> new SlotIngredient(null, n, BigDecimal.ONE, RecipeUnit.EA)).toList(),
				List.of("만든다"));
	}

	@Test
	void 이름에_없는_재료를_지운_이름으로_돌려주고_맞는_칸과_못_만듦_칸은_그대로_둔다() {
		List<GeneratedMealSlot> result = CHECK.check(List.of(slot("두부계란국", "두부"), slot("두부국", "두부"),
				GeneratedMealSlot.skipped(DAY.plusDays(1), MealType.DINNER)));

		assertThat(result).extracting(GeneratedMealSlot::title).containsExactly("두부국", "두부국", null);
	}

	@Test
	void 고친_이름이_길이_한도를_넘으면_원래_이름을_둔다() {
		// 100자 이름에서 계란(2자)을 지우면 재료 낱말이 안 남아 방울토마토(5자)를 앞에 붙인다 → 103자
		String title = "계란" + "가".repeat(GeneratedMealPlanValidator.TITLE_MAX_LENGTH - 2);
		assertThat(title).hasSize(GeneratedMealPlanValidator.TITLE_MAX_LENGTH);

		assertThat(CHECK.check(List.of(slot(title, "방울토마토"))).getFirst().title()).isEqualTo(title);
		assertThat(CHECK.check(List.of(slot("계란볶음", "감자"))).getFirst().title()).isEqualTo("감자볶음");
	}

	@Test
	void 처음_보는_주의_재료_짝이_있으면_형식_오류다() {
		assertThatThrownBy(() -> CHECK.check(List.of(slot("딸기두부", "딸기", "두부"))))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AI_INVALID_RESPONSE);
		assertThat(CHECK.check(List.of(slot("딸기우유", "딸기", "우유")))).hasSize(1);
	}
}
