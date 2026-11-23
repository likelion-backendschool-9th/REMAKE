package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다.
class MealPlanSlotTest {

	private static final LocalDate SLOT_DATE = LocalDate.of(2026, 3, 5);

	private static MealPlanSlot slot() {
		return MealPlanSlot.create(10L, SLOT_DATE, MealType.LUNCH, 100L);
	}

	@Test
	void 칸에는_항상_레시피가_있다() {
		assertThat(slot().recipeId()).isEqualTo(100L);
		assertThatThrownBy(() -> MealPlanSlot.create(10L, SLOT_DATE, MealType.LUNCH, null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 오늘보다_앞선_날짜의_칸만_지난_칸이다() {
		MealPlanSlot slot = slot();

		assertThat(slot.isPast(SLOT_DATE.plusDays(1))).isTrue();
		assertThat(slot.isPast(SLOT_DATE)).isFalse();
		assertThat(slot.isPast(SLOT_DATE.minusDays(1))).isFalse();
	}

	@Test
	void 레시피를_교체하면_새_레시피로_바뀐다() {
		MealPlanSlot slot = slot();

		slot.replaceRecipe(200L);

		assertThat(slot.recipeId()).isEqualTo(200L);
		assertThatThrownBy(() -> slot.replaceRecipe(null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 그날_요리하면_요리한_칸이_된다() {
		MealPlanSlot slot = slot();
		LocalDateTime cookedAt = SLOT_DATE.atTime(12, 30);

		slot.markCooked(cookedAt);

		assertThat(slot.isCooked()).isTrue();
		assertThat(slot.cookedAt()).isEqualTo(cookedAt);
	}

	@Test
	void 다른_날에는_요리_완료로_표시할_수_없다() {
		MealPlanSlot slot = slot();

		assertThatThrownBy(() -> slot.markCooked(SLOT_DATE.plusDays(1).atTime(9, 0)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> slot.markCooked(SLOT_DATE.minusDays(1).atTime(23, 59)))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(slot.isCooked()).isFalse();
	}
}
