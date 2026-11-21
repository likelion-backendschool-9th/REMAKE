package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

// 요약은 목록과 같은 ExpiryPolicy.statusOf로 센다. Spring 없이 확인한다.
class FridgeSummaryTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Test
	void 요약은_목록과_같은_기준으로_센다() {
		FridgeSummary summary = FridgeSummary.of(List.of(TODAY.minusDays(1), TODAY, TODAY.plusDays(1),
				TODAY.plusDays(3), TODAY.plusDays(4)), TODAY);

		assertThat(summary).isEqualTo(new FridgeSummary(5, 2, 2, 1));
	}

	@Test
	void 재료가_없으면_모두_0이다() {
		assertThat(FridgeSummary.of(List.of(), TODAY)).isEqualTo(new FridgeSummary(0, 0, 0, 0));
	}
}
