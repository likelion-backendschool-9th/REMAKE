package com.remake.quota.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.out.RecipeRewardPort;
import com.remake.quota.application.port.out.UsageLogPort;
import com.remake.quota.domain.RecipeQuota;
import com.remake.quota.domain.UsageType;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;
import com.remake.user.domain.UserPlan;

// 생성 횟수 계산과 차감. remake.quota.enforced 설정에 따라 한도를 넘었을 때 막는지, 보상 횟수를 기본 한도에서 이중 차감하지 않는지
@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceTest {

	private static final Long USER_ID = 1L;
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));

	@Mock
	private UsageLogPort usageLogPort;
	@Mock
	private RecipeRewardPort rewards;
	@Mock
	private GetUserUseCase getUserUseCase;

	private UsageQuotaService service(boolean enforced) {
		return new UsageQuotaService(usageLogPort, getUserUseCase, rewards, CLOCK, enforced);
	}

	/** 오늘 레시피 사용량. used = 기본 + 보상 합계, baseUsed = 기본 횟수로 쓴 수 */
	private void givenToday(UserPlan plan, int used, int baseUsed, int bonusRemaining) {
		User user = User.signUp(AuthProvider.KAKAO, "k-1", null).withPlan(plan);
		when(getUserUseCase.getUser(USER_ID)).thenReturn(Optional.of(user));
		when(usageLogPort.countSince(eq(USER_ID), eq(UsageType.RECIPE), any())).thenReturn(used);
		when(usageLogPort.countBaseSince(eq(USER_ID), eq(UsageType.RECIPE), any())).thenReturn(baseUsed);
		when(rewards.remaining(eq(USER_ID), any())).thenReturn(bonusRemaining);
	}

	@Test
	void 한도를_검사하면_다_쓴_뒤에는_막고_기록하지_않는다() {
		givenToday(UserPlan.BASIC, 2, 2, 0);
		UsageQuotaService service = service(true);

		assertThatThrownBy(() -> service.requireRecipeQuota(USER_ID)).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED);
		assertThatThrownBy(() -> service.consumeRecipeQuota(USER_ID)).isInstanceOf(BusinessException.class);
		verify(usageLogPort, never()).save(any(), any(), any());
	}

	@Test
	void 한도_검사를_끄면_다_쓴_뒤에도_막지_않고_기록은_남긴다() {
		givenToday(UserPlan.BASIC, 2, 2, 0);
		UsageQuotaService service = service(false);

		assertThat(service.requireRecipeQuota(USER_ID).used()).isEqualTo(2);
		assertThat(service.consumeRecipeQuota(USER_ID).used()).isEqualTo(3);
		verify(usageLogPort).save(USER_ID, UsageType.RECIPE, null);
	}

	@Test
	void 기본_2회와_보상_1회를_쓴_뒤_같은_날_PREMIUM이_되면_기본_2회가_남고_기본_한도에서_먼저_차감한다() {
		// BASIC으로 기본 2회 + 보상 1회를 쓴 뒤 PREMIUM(하루 4회)으로 바뀐 상태. 보상은 1회 더 남아 있다
		givenToday(UserPlan.PREMIUM, 3, 2, 1);
		UsageQuotaService service = service(true);

		RecipeQuota before = service.getRecipeQuota(USER_ID);
		assertThat(before.limit()).isEqualTo(4);
		assertThat(before.used()).isEqualTo(3);
		assertThat(before.remaining()).isEqualTo(3); // 기본 4 - 2 = 2 + 보상 1

		RecipeQuota after = service.consumeRecipeQuota(USER_ID);

		verify(rewards, never()).consume(any(), any());
		verify(usageLogPort).save(USER_ID, UsageType.RECIPE, null);
		assertThat(after.used()).isEqualTo(4);
		assertThat(after.baseUsed()).isEqualTo(3);
		assertThat(after.bonusRemaining()).isEqualTo(1);
		assertThat(after.remaining()).isEqualTo(2);
	}
}
