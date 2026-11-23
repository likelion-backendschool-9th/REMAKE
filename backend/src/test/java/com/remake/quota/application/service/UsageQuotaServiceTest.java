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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.out.UsageLogPort;
import com.remake.quota.domain.UsageType;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;

// remake.quota.enforced 설정에 따라 한도를 넘었을 때 막는지. BASIC 하루 2회를 이미 다 쓴 상태
@ExtendWith(MockitoExtension.class)
class UsageQuotaServiceTest {

	private static final Long USER_ID = 1L;
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-04T01:00:00Z"), ZoneId.of("Asia/Seoul"));

	@Mock
	private UsageLogPort usageLogPort;
	@Mock
	private GetUserUseCase getUserUseCase;

	@BeforeEach
	void setUp() {
		when(getUserUseCase.getUser(USER_ID)).thenReturn(Optional.of(User.signUp(AuthProvider.KAKAO, "k-1", null)));
		when(usageLogPort.countSince(eq(USER_ID), eq(UsageType.RECIPE), any())).thenReturn(2);
	}

	private UsageQuotaService service(boolean enforced) {
		return new UsageQuotaService(usageLogPort, getUserUseCase, CLOCK, enforced);
	}

	@Test
	void 한도를_검사하면_다_쓴_뒤에는_막고_기록하지_않는다() {
		UsageQuotaService service = service(true);

		assertThatThrownBy(() -> service.requireRecipeQuota(USER_ID)).isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED);
		assertThatThrownBy(() -> service.consumeRecipeQuota(USER_ID)).isInstanceOf(BusinessException.class);
		verify(usageLogPort, never()).save(any(), any(), any());
	}

	@Test
	void 한도_검사를_끄면_다_쓴_뒤에도_막지_않고_기록은_남긴다() {
		UsageQuotaService service = service(false);

		assertThat(service.requireRecipeQuota(USER_ID).used()).isEqualTo(2);
		assertThat(service.consumeRecipeQuota(USER_ID).used()).isEqualTo(3);
		verify(usageLogPort).save(USER_ID, UsageType.RECIPE, null);
	}
}
