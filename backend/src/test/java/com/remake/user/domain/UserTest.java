package com.remake.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UserTest {

	@Test
	void 온보딩_완료는_가구원_수를_넣고_완료_표시를_켠다() {
		User user = User.signUp(AuthProvider.KAKAO, "p1", "a@b.c").withId(1L);

		User completed = user.completeOnboarding(3);

		assertThat(completed.householdSize()).isEqualTo(3);
		assertThat(completed.onboardingCompleted()).isTrue();
	}

	@Test
	void 이미_온보딩을_마친_회원은_다시_완료할_수_없다() {
		User user = User.signUp(AuthProvider.KAKAO, "p1", null).withId(1L).completeOnboarding(2);

		assertThatThrownBy(() -> user.completeOnboarding(4))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void 가구원_수_범위_밖이면_예외다() {
		User user = User.signUp(AuthProvider.GOOGLE, "p1", null).withId(1L);

		assertThatThrownBy(() -> user.completeOnboarding(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> user.completeOnboarding(51)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> user.updateProfile(0, null, null)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void 설정은_보낸_필드만_바꾼다() {
		User user = User.signUp(AuthProvider.NAVER, "p1", null).withId(1L).completeOnboarding(2);

		User updated = user.updateProfile(null, true, null);

		assertThat(updated.householdSize()).isEqualTo(2);
		assertThat(updated.recipeSupplementEnabled()).isTrue();
		assertThat(updated.notificationEnabled()).isTrue();
	}
}
