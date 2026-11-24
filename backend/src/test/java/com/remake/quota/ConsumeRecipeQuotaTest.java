package com.remake.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.domain.RecipeQuota;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

// 레시피 횟수 확인·차감. 기본 시각 2026-03-04 10:00(서울), 일반 회원 하루 2회
class ConsumeRecipeQuotaTest extends IntegrationTestSupport {

	@Autowired
	private ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase;

	private int usageRows(Long userId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_logs WHERE user_id = ? AND type = 'RECIPE'",
				Integer.class, userId);
	}

	@Test
	void 차감하면_usage_logs에_한_행이_남고_남은_횟수가_준다() {
		Long userId = createUser();

		RecipeQuota after = consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		assertThat(after.used()).isEqualTo(1);
		assertThat(after.remaining()).isEqualTo(1);
		assertThat(usageRows(userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM usage_logs WHERE user_id = ?",
				LocalDateTime.class, userId)).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 0));
	}

	@Test
	void 다_쓰면_확인과_차감_모두_GENERATION_LIMIT_EXCEEDED이고_details에_남은_횟수와_초기화_시각이_있다() {
		Long userId = createUser();
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		assertThatThrownBy(() -> consumeRecipeQuotaUseCase.requireRecipeQuota(userId))
				.isInstanceOfSatisfying(BusinessException.class, e -> {
					assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED);
					assertThat(e.getDetails()).containsEntry("remaining", 0)
							.containsEntry("resetsAt", LocalDateTime.of(2026, 3, 5, 0, 0));
				});
		assertThatThrownBy(() -> consumeRecipeQuotaUseCase.consumeRecipeQuota(userId))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED));
		assertThat(usageRows(userId)).isEqualTo(2);
	}

	@Test
	void 한국_시간_자정이_지나면_다시_쓸_수_있다() {
		Long userId = createUser();
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		clock.setNow(ZonedDateTime.of(2026, 3, 5, 0, 0, 0, 0, MutableClock.ZONE));

		assertThat(consumeRecipeQuotaUseCase.requireRecipeQuota(userId).remaining()).isEqualTo(2);
	}

	@Test
	void 같은_회원이_동시에_차감해도_한도를_넘지_않는다() throws Exception {
		Long userId = createUser();
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		// 남은 횟수 1. 첫 요청이 차감하고 커밋하기 전에 두 번째 요청이 들어온다.
		CountDownLatch firstConsumed = new CountDownLatch(1);

		CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(() -> transactionTemplate.execute(status -> {
			consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
			firstConsumed.countDown();
			sleep(500);
			return true;
		}));
		assertThat(firstConsumed.await(5, TimeUnit.SECONDS)).isTrue();
		CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(() -> {
			try {
				consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
				return true;
			} catch (BusinessException e) {
				return false;
			}
		});

		assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
		assertThat(second.get(10, TimeUnit.SECONDS)).as("첫 요청이 커밋될 때까지 기다린 뒤 한도 초과").isFalse();
		assertThat(usageRows(userId)).isEqualTo(2);
	}

	@Test
	void 다른_회원끼리는_서로_기다리지_않는다() {
		Long me = createUser();
		Long other = createUser();

		transactionTemplate.executeWithoutResult(status -> {
			consumeRecipeQuotaUseCase.consumeRecipeQuota(me);
			// 같은 트랜잭션 안에서 다른 회원 잠금은 바로 얻는다(막히면 테스트가 멈춘다)
			consumeRecipeQuotaUseCase.consumeRecipeQuota(other);
		});

		assertThat(usageRows(me)).isEqualTo(1);
		assertThat(usageRows(other)).isEqualTo(1);
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
