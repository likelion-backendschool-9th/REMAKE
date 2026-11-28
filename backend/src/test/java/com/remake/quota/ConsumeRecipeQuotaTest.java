package com.remake.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
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
	void 다른_회원끼리는_서로_기다리지_않는다() throws Exception {
		Long me = createUser();
		Long other = createUser();
		// 내 차감 트랜잭션이 잠금을 쥔 채 끝나지 않은 동안 다른 회원이 별도 트랜잭션으로 차감한다.
		CountDownLatch meConsumed = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);

		CompletableFuture<Void> mine = CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
			consumeRecipeQuotaUseCase.consumeRecipeQuota(me);
			meConsumed.countDown();
			awaitQuietly(release);
		}));
		try {
			assertThat(meConsumed.await(5, TimeUnit.SECONDS)).isTrue();
			CompletableFuture<Void> others = CompletableFuture
					.runAsync(() -> consumeRecipeQuotaUseCase.consumeRecipeQuota(other));
			assertThatCode(() -> others.get(2, TimeUnit.SECONDS)).as("내 잠금이 풀리기 전에 끝난다")
					.doesNotThrowAnyException();
		} finally {
			release.countDown();
		}
		mine.get(10, TimeUnit.SECONDS);

		assertThat(usageRows(me)).isEqualTo(1);
		assertThat(usageRows(other)).isEqualTo(1);
	}

	private void changePlan(Long userId, String plan) {
		jdbcTemplate.update("UPDATE users SET plan = ? WHERE id = ?", plan, userId);
	}

	@Test
	void 일반에서_구독으로_바뀌면_오늘_쓴_횟수를_빼고_구독_한도만큼_남는다() {
		Long userId = createUser();
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		changePlan(userId, "PREMIUM");

		assertThat(consumeRecipeQuotaUseCase.requireRecipeQuota(userId).remaining()).isEqualTo(2);
		RecipeQuota after = consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		assertThat(after.used()).isEqualTo(3);
		assertThat(after.remaining()).isEqualTo(1);
		assertThat(usageRows(userId)).isEqualTo(3);
	}

	@Test
	void 구독에서_일반으로_바뀌어_이미_한도를_넘었으면_막고_기록은_그대로이며_남은_횟수는_음수가_아니다() {
		Long userId = createUser();
		changePlan(userId, "PREMIUM");
		for (int i = 0; i < 4; i++) {
			consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		}

		changePlan(userId, "BASIC");

		assertThatThrownBy(() -> consumeRecipeQuotaUseCase.requireRecipeQuota(userId))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED));
		assertThatThrownBy(() -> consumeRecipeQuotaUseCase.consumeRecipeQuota(userId))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GENERATION_LIMIT_EXCEEDED));
		assertThat(usageRows(userId)).isEqualTo(4);
	}

	@Test
	void 보상을_쓴_날_일반에서_구독으로_바뀌어도_기본_횟수를_이중_차감하지_않고_보상은_그대로다() {
		Long userId = createUser();
		// ck_quota_grants_reward 때문에 amount는 순위별 고정값(3순위=3)이다. 남은 횟수만 2로 둔다.
		jdbcTemplate.update("""
				INSERT INTO generation_quota_grants
				(user_id, shared_recipe_id, week_start, rank_no, amount, remaining, expires_at, created_at)
				VALUES (?, NULL, ?, 3, 3, 2, ?, ?)
				""", userId, LocalDate.of(2026, 2, 23), LocalDateTime.of(2026, 3, 31, 0, 0),
				LocalDateTime.of(2026, 3, 1, 0, 0));
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		changePlan(userId, "PREMIUM");

		RecipeQuota quota = consumeRecipeQuotaUseCase.requireRecipeQuota(userId);
		assertThat(quota.used()).isEqualTo(3);
		assertThat(quota.baseUsed()).isEqualTo(2);
		assertThat(quota.bonusRemaining()).isEqualTo(1);
		assertThat(quota.remaining()).isEqualTo(3);

		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM generation_quota_grants WHERE user_id = ?",
				Integer.class, userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT grant_id FROM usage_logs WHERE user_id = ? ORDER BY id DESC LIMIT 1", Long.class, userId))
				.isNull();
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
