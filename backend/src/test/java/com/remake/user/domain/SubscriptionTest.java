package com.remake.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class SubscriptionTest {

	@Test
	void 다음_결제일은_한_달_뒤이고_말일이_없으면_말일이다() {
		assertThat(Subscription.nextBillingDate(LocalDate.of(2026, 1, 15)))
				.isEqualTo(LocalDate.of(2026, 2, 15));
		assertThat(Subscription.nextBillingDate(LocalDate.of(2026, 1, 31)))
				.isEqualTo(LocalDate.of(2026, 2, 28));
		assertThat(Subscription.nextBillingDate(LocalDate.of(2024, 1, 31)))
				.isEqualTo(LocalDate.of(2024, 2, 29));
	}

	@Test
	void 해지_예약은_혜택을_유지하고_반복_호출은_그대로다() {
		LocalDateTime created = LocalDateTime.of(2026, 3, 4, 10, 0);
		Subscription active = Subscription.start(1L, "cust-1", "enc-key", LocalDate.of(2026, 3, 4), created);
		LocalDateTime canceledAt = created.plusDays(3);

		Subscription canceled = active.scheduleCancel(canceledAt);
		assertThat(canceled.status()).isEqualTo(SubscriptionStatus.CANCELED);
		assertThat(canceled.canceledAt()).isEqualTo(canceledAt);
		assertThat(canceled.billingKeyEncrypted()).isEqualTo("enc-key");
		assertThat(canceled.scheduleCancel(canceledAt.plusDays(1))).isSameAs(canceled);
	}

	@Test
	void 종료하면_billingKey를_비운다() {
		Subscription active = Subscription.start(1L, "cust-1", "enc-key", LocalDate.of(2026, 3, 4),
				LocalDateTime.of(2026, 3, 4, 10, 0));
		LocalDateTime endedAt = LocalDateTime.of(2026, 3, 10, 10, 0);

		Subscription ended = active.end(endedAt);
		assertThat(ended.status()).isEqualTo(SubscriptionStatus.ENDED);
		assertThat(ended.billingKeyEncrypted()).isNull();
		assertThat(ended.endedAt()).isEqualTo(endedAt);
	}

	@Test
	void 종료된_구독은_해지할_수_없다() {
		Subscription ended = Subscription.start(1L, "cust-1", "enc-key", LocalDate.of(2026, 3, 4),
				LocalDateTime.of(2026, 3, 4, 10, 0)).end(LocalDateTime.of(2026, 3, 5, 10, 0));

		assertThatThrownBy(() -> ended.scheduleCancel(LocalDateTime.of(2026, 3, 6, 10, 0)))
				.isInstanceOf(IllegalStateException.class);
	}
}
