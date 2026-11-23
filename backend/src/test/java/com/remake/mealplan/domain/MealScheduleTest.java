package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다.
class MealScheduleTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 25);

	@Test
	void 일수는_1일부터_최대_일수까지다() {
		Set<MealType> dinner = EnumSet.of(MealType.DINNER);

		assertThat(new MealSchedule(START, 1, dinner, Set.of()).dates()).containsExactly(START);
		assertThat(new MealSchedule(START, MealPlan.MAX_DAYS, dinner, Set.of()).dates())
				.hasSize(MealPlan.MAX_DAYS).endsWith(START.plusDays(MealPlan.MAX_DAYS - 1L));
		assertThatThrownBy(() -> new MealSchedule(START, 0, dinner, Set.of()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new MealSchedule(START, MealPlan.MAX_DAYS + 1, dinner, Set.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 끼니가_없으면_거절한다() {
		assertThatThrownBy(() -> new MealSchedule(START, 1, EnumSet.noneOf(MealType.class), Set.of()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 기간_안의_날짜인지_안다() {
		MealSchedule schedule = new MealSchedule(START, 3, EnumSet.of(MealType.DINNER), null);

		assertThat(schedule.contains(START)).isTrue();
		assertThat(schedule.contains(START.plusDays(2))).isTrue();
		assertThat(schedule.contains(START.plusDays(3))).isFalse();
		assertThat(schedule.contains(START.minusDays(1))).isFalse();
	}
}
