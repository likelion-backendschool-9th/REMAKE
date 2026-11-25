package com.remake.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AccountDeletionRequestTest {

	private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 4, 10, 0);

	@Test
	void 다섯_번_틀리면_잠긴다() {
		AccountDeletionRequest request = AccountDeletionRequest.issue(1L, "hash", NOW.plusMinutes(5), NOW);

		for (int i = 0; i < 4; i++) {
			assertThat(request.registerWrongAttempt()).isFalse();
			assertThat(request.isLocked()).isFalse();
		}
		assertThat(request.registerWrongAttempt()).isTrue();
		assertThat(request.isLocked()).isTrue();
		assertThat(request.remainingAttempts()).isZero();
	}

	@Test
	void 만료는_expiresAt이_지금과_같거나_이전이면_true다() {
		AccountDeletionRequest request = AccountDeletionRequest.issue(1L, "hash", NOW.plusMinutes(5), NOW);

		assertThat(request.isExpired(NOW.plusMinutes(4))).isFalse();
		assertThat(request.isExpired(NOW.plusMinutes(5))).isTrue();
		assertThat(request.isExpired(NOW.plusMinutes(6))).isTrue();
	}
}
