package com.remake.ingredient.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

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

	@Test
	void 소비기한을_모르면_기준일에_기본_보관_일수를_더해_추정한다() {
		assertThat(ExpiryPolicy.estimateExpirationDate(TODAY, 7)).isEqualTo(LocalDate.of(2026, 3, 11));
		// 영수증은 구매일이 기준이라 오래된 구매일이면 추정한 날짜가 이미 지났을 수 있다.
		assertThat(ExpiryPolicy.estimateExpirationDate(LocalDate.of(2026, 2, 20), 3)).isEqualTo(LocalDate.of(2026, 2, 23));
	}

	@Test
	void D_DAY와_지난_날은_위험이고_D1부터_D3은_임박이고_그_뒤는_여유다() {
		assertThat(ExpiryPolicy.statusOf(-1)).isEqualTo(ExpiryStatus.DANGER);
		assertThat(ExpiryPolicy.statusOf(0)).isEqualTo(ExpiryStatus.DANGER);
		assertThat(ExpiryPolicy.statusOf(1)).isEqualTo(ExpiryStatus.SOON);
		assertThat(ExpiryPolicy.statusOf(3)).isEqualTo(ExpiryStatus.SOON);
		assertThat(ExpiryPolicy.statusOf(4)).isEqualTo(ExpiryStatus.OK);
	}

	@Test
	void dDay는_오늘이_기한이면_0이고_지났으면_음수다() {
		assertThat(ExpiryPolicy.dDay(TODAY, TODAY)).isZero();
		assertThat(ExpiryPolicy.dDay(TODAY.plusDays(3), TODAY)).isEqualTo(3);
		assertThat(ExpiryPolicy.dDay(TODAY.minusDays(2), TODAY)).isEqualTo(-2);
		assertThat(ExpiryPolicy.dDay(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 2, 27))).isEqualTo(3);
	}

	@Test
	void 요약은_목록과_같은_기준으로_센다() {
		FridgeSummary summary = FridgeSummary.of(List.of(TODAY.minusDays(1), TODAY, TODAY.plusDays(1),
				TODAY.plusDays(3), TODAY.plusDays(4)), TODAY);

		assertThat(summary).isEqualTo(new FridgeSummary(5, 2, 2, 1));
		assertThat(FridgeSummary.of(List.of(), TODAY)).isEqualTo(new FridgeSummary(0, 0, 0, 0));
	}
}
