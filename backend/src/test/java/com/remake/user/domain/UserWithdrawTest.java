package com.remake.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserWithdrawTest {

	@Test
	void 탈퇴하면_소셜_식별자와_이메일을_비운다() {
		LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 4, 10, 0);
		User user = User.signUp(AuthProvider.KAKAO, "kakao-1", "a@b.c").withId(1L).withdraw(deletedAt);

		assertThat(user.providerUserId()).isNull();
		assertThat(user.email()).isNull();
		assertThat(user.deletedAt()).isEqualTo(deletedAt);
		assertThat(user.isActive()).isFalse();
	}

	@Test
	void 이미_탈퇴한_회원은_다시_탈퇴할_수_없다() {
		LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 4, 10, 0);
		User user = User.signUp(AuthProvider.GOOGLE, "g-1", "a@b.c").withId(1L).withdraw(deletedAt);

		assertThatThrownBy(() -> user.withdraw(deletedAt.plusDays(1)))
				.isInstanceOf(IllegalStateException.class);
	}
}
