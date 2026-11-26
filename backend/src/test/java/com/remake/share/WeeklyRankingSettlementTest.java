package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.share.application.port.in.FinalizeWeeklyRankingUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

class WeeklyRankingSettlementTest extends IntegrationTestSupport {
	@Autowired
	private FinalizeWeeklyRankingUseCase settlement;
	@Autowired
	private GetUsageQuotasUseCase quotas;
	@Autowired
	private ConsumeRecipeQuotaUseCase consume;
	private LocalDateTime end;
	private Long author;

	@BeforeEach
	void setUp() {
		clock.setNow(ZonedDateTime.of(2026, 8, 31, 0, 0, 0, 0, MutableClock.ZONE));
		end = LocalDateTime.now(clock);
		author = createUser();
	}

	@Test
	void 지난주_유효저장으로_확정하고_동일작성자의_여러순위에_각각_보상한다() {
		Long first = post(author, end.minusWeeks(3));
		Long second = post(author, end.minusWeeks(2));
		Long third = post(createUser(), end.minusDays(3));
		save(first, createUser(), end.minusWeeks(1));
		save(first, createUser(), end.minusNanos(1000));
		save(second, createUser(), end.minusDays(1));
		save(third, createUser(), end.minusDays(1));
		var result = settlement.finalizePreviousWeek();
		assertThat(result.weekStart()).isEqualTo(LocalDate.of(2026, 8, 24));
		assertThat(result.ranks()).isEqualTo(3);
		assertThat(result.rewards()).isEqualTo(3);
		assertThat(jdbcTemplate.queryForList("SELECT shared_recipe_id FROM shared_recipe_weekly_ranks ORDER BY rank_no", Long.class))
				.containsExactly(first, second, third);
		assertThat(jdbcTemplate.queryForList("SELECT amount FROM generation_quota_grants ORDER BY rank_no", Integer.class))
				.containsExactly(10, 6, 3);
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isEqualTo(16);
		assertThat(jdbcTemplate.queryForList("SELECT expires_at FROM generation_quota_grants", LocalDateTime.class))
				.containsOnly(LocalDateTime.of(2026, 9, 30, 0, 0));
		awaitEventWrites();
		assertThat(count("events")).isEqualTo(3);
		assertThat(jdbcTemplate.queryForList("SELECT event_name FROM events", String.class)).containsOnly("weekly_reward_granted");
		assertThat(jdbcTemplate.queryForList("SELECT properties->>'reward_amount' FROM events", String.class))
				.containsExactlyInAnyOrder("10", "6", "3");
		assertThat(jdbcTemplate.queryForList("SELECT source FROM events", String.class)).containsOnly("SERVER");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE screen IS NULL", Integer.class)).isEqualTo(3);
	}

	@Test
	void 본인저장_취소_기간밖_숨김_저장0건은_확정대상이_아니다() {
		Long own = post(author, end.minusDays(6));
		save(own, author, end.minusDays(1));
		Long outside = post(author, end.minusDays(5));
		save(outside, createUser(), end.minusWeeks(1).minusSeconds(1));
		save(outside, createUser(), end);
		Long canceled = post(author, end.minusDays(4));
		save(canceled, createUser(), end.minusDays(1));
		jdbcTemplate.update("DELETE FROM saved_recipes WHERE shared_recipe_id=?", canceled);
		Long hidden = post(author, end.minusDays(3));
		save(hidden, createUser(), end.minusDays(1));
		jdbcTemplate.update("UPDATE shared_recipes SET status='HIDDEN' WHERE id=?", hidden);
		post(author, end.minusDays(2));
		var result = settlement.finalizePreviousWeek();
		assertThat(result.ranks()).isZero();
		assertThat(result.rewards()).isZero();
		assertThat(count("shared_recipe_weekly_settlements")).isEqualTo(1);
		awaitEventWrites();
		assertThat(count("events")).isZero();
	}

	@Test
	void 재실행은_순위_보상_이벤트를_바꾸지_않는다() {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		jdbcTemplate.update("DELETE FROM saved_recipes WHERE shared_recipe_id=?", post);
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertThat(count("shared_recipe_weekly_ranks")).isEqualTo(1);
		assertThat(count("generation_quota_grants")).isEqualTo(1);
		awaitEventWrites();
		assertThat(count("events")).isEqualTo(1);
	}

	@Test
	void 빈주도_확정하여_나중에_과거저장이_들어와도_다시_지급하지_않는다() {
		assertThat(settlement.finalizePreviousWeek().finalized()).isTrue();
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertThat(count("generation_quota_grants")).isZero();
	}

	@Test
	void 동시실행해도_한번만_확정한다() throws Exception {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		try (var pool = Executors.newFixedThreadPool(2)) {
			var start = new CountDownLatch(1);
			var first = pool.submit(() -> { start.await(); return settlement.finalizePreviousWeek(); });
			var second = pool.submit(() -> { start.await(); return settlement.finalizePreviousWeek(); });
			start.countDown();
			assertThat(first.get(10, TimeUnit.SECONDS).finalized())
					.isNotEqualTo(second.get(10, TimeUnit.SECONDS).finalized());
		}
		assertThat(count("shared_recipe_weekly_ranks")).isEqualTo(1);
		assertThat(count("generation_quota_grants")).isEqualTo(1);
		awaitEventWrites();
		assertThat(count("events")).isEqualTo(1);
	}

	@Test
	void 두번째_보상에서_실패하면_첫보상_순위_완료기록까지_롤백하고_재시도한다() {
		for (int i = 0; i < 2; i++) {
			Long post = post(author, end.minusDays(3 - i));
			save(post, createUser(), end.minusHours(1));
		}
		jdbcTemplate.execute("ALTER TABLE generation_quota_grants ADD CONSTRAINT test_fail_second CHECK (rank_no <> 2) NOT VALID");
		try {
			assertThatThrownBy(settlement::finalizePreviousWeek).isInstanceOf(RuntimeException.class);
			assertThat(count("generation_quota_grants")).isZero();
			assertThat(count("shared_recipe_weekly_ranks")).isZero();
			assertThat(count("shared_recipe_weekly_settlements")).isZero();
			awaitEventWrites();
			assertThat(count("events")).isZero();
		} finally {
			jdbcTemplate.execute("ALTER TABLE generation_quota_grants DROP CONSTRAINT test_fail_second");
		}
		assertThat(settlement.finalizePreviousWeek().rewards()).isEqualTo(2);
	}

	@Test
	void 탈퇴자는_순위에_남지만_보상은_지급하지_않고_차순위로_넘기지_않는다() {
		Long post = post(author, end.minusDays(2));
		save(post, createUser(), end.minusHours(1));
		jdbcTemplate.update("UPDATE users SET deleted_at=? WHERE id=?", end.minusMinutes(1), author);
		var result = settlement.finalizePreviousWeek();
		assertThat(result.ranks()).isEqualTo(1);
		assertThat(result.rewards()).isZero();
	}

	@Test
	void 백위까지만_확정하고_상위세개만_지급한다() {
		Long reader = createUser();
		for (int i = 0; i < 101; i++) {
			Long post = post(author, end.minusDays(6).plusMinutes(i));
			save(post, reader, end.minusHours(1));
		}
		var result = settlement.finalizePreviousWeek();
		assertThat(result.ranks()).isEqualTo(100);
		assertThat(result.rewards()).isEqualTo(3);
		assertThat(jdbcTemplate.queryForObject("SELECT MAX(rank_no) FROM shared_recipe_weekly_ranks", Integer.class)).isEqualTo(100);
	}

	@Test
	void UTC로_입력한_시각에서도_KST_월요일과_연경계를_계산한다() {
		clock.setNow(ZonedDateTime.parse("2027-01-03T15:00:00Z"));
		Long post = post(author, LocalDateTime.of(2026, 12, 28, 0, 0));
		save(post, createUser(), LocalDateTime.of(2027, 1, 3, 23, 59));
		assertThat(settlement.finalizePreviousWeek().weekStart()).isEqualTo(LocalDate.of(2026, 12, 28));
		assertThat(count("generation_quota_grants")).isEqualTo(1);
	}

	@Test
	void 기본횟수부터_쓰고_보상을_차감하며_다음날은_기본횟수가_다시_생긴다() {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isEqualTo(10);
		assertThat(consume.consumeRecipeQuota(author).bonusRemaining()).isEqualTo(9);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM usage_logs WHERE grant_id IS NOT NULL", Integer.class)).isEqualTo(1);
		clock.setNow(ZonedDateTime.of(2026, 9, 1, 0, 0, 0, 0, MutableClock.ZONE));
		assertThat(quotas.getUsageQuotas(author).recipe().remaining()).isEqualTo(11);
		consume.consumeRecipeQuota(author);
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isEqualTo(9);
	}

	@Test
	void 만료시각부터_보상을_사용하지_못한다() {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		clock.setNow(ZonedDateTime.of(2026, 9, 30, 0, 0, 0, 0, MutableClock.ZONE));
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isZero();
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		assertThatThrownBy(() -> consume.consumeRecipeQuota(author)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void 여러지급건은_만료가_가까운것부터_사용하고_차감실패는_되돌린다() {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		Long grant = jdbcTemplate.queryForObject("SELECT id FROM generation_quota_grants", Long.class);
		jdbcTemplate.update("""
				INSERT INTO generation_quota_grants
				(user_id, shared_recipe_id, week_start, rank_no, amount, remaining, expires_at, created_at)
				VALUES (?, ?, ?, 2, 6, 6, ?, ?)
				""", author, post, end.toLocalDate().minusWeeks(2), end.plusDays(1), end);
		Long early = jdbcTemplate.queryForObject("SELECT id FROM generation_quota_grants WHERE rank_no=2", Long.class);
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
			consume.consumeRecipeQuota(author);
			throw new IllegalStateException("레시피 저장 실패");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isEqualTo(16);
		assertThat(count("usage_logs")).isEqualTo(2);
		consume.consumeRecipeQuota(author);
		assertThat(jdbcTemplate.queryForObject("SELECT grant_id FROM usage_logs WHERE grant_id IS NOT NULL", Long.class)).isEqualTo(early);
		assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM generation_quota_grants WHERE id=?", Integer.class, grant)).isEqualTo(10);
	}

	@Test
	void 마지막보상_동시차감은_한요청만_성공한다() throws Exception {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		jdbcTemplate.update("UPDATE generation_quota_grants SET remaining=1");
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var start = new CountDownLatch(1);
			java.util.concurrent.Callable<Boolean> attempt = () -> {
				start.await();
				try {
					consume.consumeRecipeQuota(author);
					return true;
				} catch (com.remake.global.error.BusinessException ex) {
					assertThat(ex.getErrorCode()).isEqualTo(com.remake.global.error.ErrorCode.GENERATION_LIMIT_EXCEEDED);
					return false;
				}
			};
			var first = pool.submit(attempt);
			var second = pool.submit(attempt);
			start.countDown();
			assertThat(first.get(10, TimeUnit.SECONDS)).isNotEqualTo(second.get(10, TimeUnit.SECONDS));
		}
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isZero();
		assertThat(count("usage_logs")).isEqualTo(3);
	}

	@Test
	void 다음주는_별도로_확정하고_지난주_집계에는_섞이지_않는다() {
		Long post = post(author, end.minusDays(1));
		save(post, createUser(), end.minusHours(1));
		settlement.finalizePreviousWeek();
		save(post, createUser(), end);
		clock.setNow(ZonedDateTime.of(2026, 9, 7, 0, 0, 0, 0, MutableClock.ZONE));
		var result = settlement.finalizePreviousWeek();
		assertThat(result.weekStart()).isEqualTo(LocalDate.of(2026, 8, 31));
		assertThat(result.finalized()).isTrue();
		assertThat(count("shared_recipe_weekly_settlements")).isEqualTo(2);
		assertThat(jdbcTemplate.queryForList("SELECT save_count FROM shared_recipe_weekly_ranks", Integer.class)).containsOnly(1);
		assertThat(quotas.getUsageQuotas(author).recipe().bonusRemaining()).isEqualTo(20);
	}

	private int count(String table) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
	}

	private Long post(Long owner, LocalDateTime sharedAt) {
		Long recipe = createRecipe(owner);
		return jdbcTemplate.queryForObject("""
				INSERT INTO shared_recipes (recipe_id, title, image_key, status, created_at, updated_at)
				VALUES (?, '주간 보상', 'reward-test.jpg', 'VISIBLE', ?, ?) RETURNING id
				""", Long.class, recipe, sharedAt, sharedAt);
	}

	private void save(Long post, Long user, LocalDateTime savedAt) {
		jdbcTemplate.update("""
				INSERT INTO saved_recipes (user_id, recipe_id, shared_recipe_id, saved_at)
				SELECT ?, recipe_id, id, ? FROM shared_recipes WHERE id=?
				""", user, savedAt, post);
	}
}
