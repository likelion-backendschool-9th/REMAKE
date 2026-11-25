package com.remake.mealplan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Test;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.recipe.domain.RecipeUnit;

// AI 응답 검사: 칸 하나라도 망가지면 응답 전체가 AI_INVALID_RESPONSE(서버 작업이 1회 재시도).
class GeneratedMealPlanValidatorTest {

	private static final LocalDate START = LocalDate.of(2026, 3, 4);

	/** 2일 × 아침·저녁, 첫날 아침은 남기는 칸 → 채울 칸 3개 */
	private static final MealSchedule SCHEDULE = new MealSchedule(START, 2, Set.of(MealType.BREAKFAST, MealType.DINNER),
			Set.of(new MealSlotKey(START, MealType.BREAKFAST)));

	private static GeneratedMealSlot slot(LocalDate date, MealType mealType) {
		return new GeneratedMealSlot(date, mealType, "대파 볶음", 15, null,
				List.of(new SlotIngredient(1L, "대파", new BigDecimal("100"), RecipeUnit.G)), List.of("볶는다."));
	}

	private static List<GeneratedMealSlot> validSlots() {
		return new ArrayList<>(List.of(
				slot(START, MealType.DINNER),
				slot(START.plusDays(1), MealType.BREAKFAST),
				slot(START.plusDays(1), MealType.DINNER)));
	}

	/** 두 번째 칸만 바꾼다. */
	private static List<GeneratedMealSlot> withSecond(UnaryOperator<GeneratedMealSlot> change) {
		List<GeneratedMealSlot> slots = validSlots();
		slots.set(1, change.apply(slots.get(1)));
		return slots;
	}

	private static GeneratedMealSlot copy(GeneratedMealSlot s, String title, int cookMinutes, String tips,
			List<SlotIngredient> ingredients, List<String> steps) {
		return new GeneratedMealSlot(s.date(), s.mealType(), title, cookMinutes, tips, ingredients, steps);
	}

	private static void assertInvalid(List<GeneratedMealSlot> slots) {
		assertThatThrownBy(() -> GeneratedMealPlanValidator.validate(SCHEDULE, slots))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_INVALID_RESPONSE));
	}

	@Test
	void 채울_칸이_모두_한_번씩_있고_레시피가_멀쩡하면_그대로_통과한다() {
		List<GeneratedMealSlot> slots = validSlots();

		assertThat(GeneratedMealPlanValidator.validate(SCHEDULE, slots)).isEqualTo(slots);
	}

	@Test
	void 칸이_빠지거나_같은_칸이_두_번이거나_요청하지_않은_칸이면_실패한다() {
		List<GeneratedMealSlot> missing = validSlots();
		missing.remove(2);
		List<GeneratedMealSlot> duplicated = validSlots();
		duplicated.set(2, slot(START.plusDays(1), MealType.BREAKFAST));
		List<GeneratedMealSlot> keptSlot = validSlots();
		keptSlot.add(slot(START, MealType.BREAKFAST));
		List<GeneratedMealSlot> outOfRange = validSlots();
		outOfRange.add(slot(START.plusDays(2), MealType.DINNER));

		assertInvalid(List.of());
		assertInvalid(missing);
		assertInvalid(duplicated);
		assertInvalid(keptSlot);
		assertInvalid(outOfRange);
	}

	@Test
	void 칸_하나의_레시피가_망가지면_응답_전체가_실패한다() {
		assertInvalid(withSecond(s -> copy(s, " ", 15, null, s.ingredients(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, null, 15, null, s.ingredients(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, "가".repeat(101), 15, null, s.ingredients(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 0, null, s.ingredients(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, "가".repeat(1001), s.ingredients(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null, List.of(), s.steps())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null, s.ingredients(), List.of())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null, s.ingredients(), List.of("볶는다.", " "))));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null, s.ingredients(), List.of("가".repeat(1001)))));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null,
				List.of(new SlotIngredient(null, "가".repeat(101), BigDecimal.ONE, RecipeUnit.G)), s.steps())));
		assertInvalid(withSecond(s -> copy(s, s.title(), 15, null,
				List.of(new SlotIngredient(null, "대파", new BigDecimal("12345678"), RecipeUnit.G)), s.steps())));
	}

	@Test
	void 못_만듦_칸은_있는_칸으로_세고_레시피는_검사하지_않는다() {
		List<GeneratedMealSlot> slots = validSlots();
		slots.set(1, GeneratedMealSlot.skipped(START.plusDays(1), MealType.BREAKFAST));
		slots.set(2, GeneratedMealSlot.skipped(START.plusDays(1), MealType.DINNER));

		assertThat(GeneratedMealPlanValidator.validate(SCHEDULE, slots)).hasSize(3);
	}

	@Test
	void 한도_끝값은_통과한다() {
		List<GeneratedMealSlot> slots = withSecond(s -> copy(s, "가".repeat(100), 1, "가".repeat(1000),
				List.of(new SlotIngredient(null, "가".repeat(100), new BigDecimal("1234567.123"), RecipeUnit.G)),
				List.of("가".repeat(1000))));

		assertThat(GeneratedMealPlanValidator.validate(SCHEDULE, slots)).hasSize(3);
	}
}
