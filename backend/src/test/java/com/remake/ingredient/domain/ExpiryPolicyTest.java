package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// 도메인 규칙: Spring 없이 확인한다. 오늘 날짜는 인수로 넣는다.
class ExpiryPolicyTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Test
	void 임박_기준은_3일이다() {
		assertThat(ExpiryPolicy.SOON_DAYS).isEqualTo(3);
	}

	@Test
	void 임박_범위는_오늘부터_3일_뒤까지다() {
		assertThat(ExpiryPolicy.expiringFrom(TODAY)).isEqualTo(LocalDate.of(2026, 3, 4));
		assertThat(ExpiryPolicy.expiringTo(TODAY)).isEqualTo(LocalDate.of(2026, 3, 7));
	}

	@Test
	void 만료는_오늘보다_이전이고_어제_만료는_어제다() {
		assertThat(ExpiryPolicy.expiredBefore(TODAY)).isEqualTo(TODAY);
		assertThat(ExpiryPolicy.expiredYesterday(TODAY)).isEqualTo(LocalDate.of(2026, 3, 3));
	}

	@Test
	void 달이_바뀌어도_날짜를_올바르게_계산한다() {
		LocalDate monthEnd = LocalDate.of(2026, 2, 28);

		assertThat(ExpiryPolicy.expiringTo(monthEnd)).isEqualTo(LocalDate.of(2026, 3, 3));
		assertThat(ExpiryPolicy.expiredYesterday(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2026, 2, 28));
	}
}
