package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.share.application.port.in.DeleteSharedRecipeUseCase;
import com.remake.share.application.port.in.FinalizeWeeklyRankingUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

class WeeklyRankingDeletionSettlementTest extends IntegrationTestSupport {
	@Autowired
	private FinalizeWeeklyRankingUseCase settlement;
	@Autowired
	private DeleteSharedRecipeUseCase deletion;
	@Autowired
	private ConsumeRecipeQuotaUseCase consume;
	private Long author;
	private LocalDateTime boundary;

	@BeforeEach
	void setUp() {
		clock.setNow(ZonedDateTime.of(2026, 8, 31, 0, 0, 0, 0, MutableClock.ZONE));
		boundary = LocalDateTime.now(clock);
		author = createUser();
	}

	@Test
	void 확정전_삭제한_상위글을_빼고_남은_글에만_순위와_보상을_지급한다() {
		Long removed = post(0);
		Long retained = post(1);
		deletion.delete(author, removed);
		var result = settlement.finalizePreviousWeek();
		assertThat(result.ranks()).isEqualTo(1);
		assertThat(result.rewards()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForList("SELECT shared_recipe_id FROM shared_recipe_weekly_ranks", Long.class))
				.containsExactly(retained);
		assertThat(jdbcTemplate.queryForList("SELECT amount FROM generation_quota_grants", Integer.class))
				.containsExactly(10);
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertEvents(1);
	}

	@Test
	void 전부_삭제된_주도_빈주로_확정하고_재실행하지_않는다() {
		deletion.delete(author, post(0));
		assertThat(settlement.finalizePreviousWeek().ranks()).isZero();
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertCounts(1, 0, 0);
		assertEvents(0);
	}

	@Test
	void 확정후_삭제해도_사용한_보상과_확정이력을_보존하고_과거순위를_당기지_않는다() throws Exception {
		Long first = post(0);
		Long second = post(1);
		post(2);
		post(3);
		assertThat(settlement.finalizePreviousWeek().rewards()).isEqualTo(3);
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		consume.consumeRecipeQuota(author);
		var ranks = jdbcTemplate.queryForList("SELECT * FROM shared_recipe_weekly_ranks ORDER BY rank_no");
		var grants = jdbcTemplate.queryForList("SELECT * FROM generation_quota_grants ORDER BY rank_no");
		var completed = jdbcTemplate.queryForList("SELECT * FROM shared_recipe_weekly_settlements");
		assertThat(grants).extracting(row -> row.get("remaining")).containsExactly(9, 6, 3);
		deletion.delete(author, first);
		deletion.delete(author, first);
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertThat(jdbcTemplate.queryForList("SELECT * FROM shared_recipe_weekly_ranks ORDER BY rank_no")).isEqualTo(ranks);
		assertThat(jdbcTemplate.queryForList("SELECT * FROM generation_quota_grants ORDER BY rank_no")).isEqualTo(grants);
		assertThat(jdbcTemplate.queryForList("SELECT * FROM shared_recipe_weekly_settlements")).isEqualTo(completed);
		assertEvents(3);

		// 공개 과거 전용 API는 없다. 실제 공개 조회의 지난주 비교 값은 원래 2위를 유지한다.
		save(first, createUser(), boundary);
		save(second, createUser(), boundary);
		mockMvc.perform(get("/api/v1/user/shared-recipes/ranking")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, author))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(second))
				.andExpect(jsonPath("$.content[0].rank").value(1))
				.andExpect(jsonPath("$.content[0].rankChange").value(1));
	}

	@Test
	void 삭제가_먼저_커밋되면_대기하던_확정에서_제외한다() throws Exception {
		Long post = post(0);
		race(post, true, false);
		assertDeleted(post);
		assertCounts(1, 0, 0);
		assertEvents(0);
	}

	@Test
	void 삭제가_먼저_롤백되면_대기하던_확정에서_정상지급한다() throws Exception {
		Long post = post(0);
		race(post, true, true);
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM shared_recipes WHERE id=?", String.class, post))
				.isEqualTo("VISIBLE");
		assertCounts(1, 1, 1);
		assertEvents(1);
	}

	@Test
	void 확정이_먼저_커밋되면_대기하던_삭제후에도_보상을_유지한다() throws Exception {
		Long post = post(0);
		race(post, false, false);
		assertDeleted(post);
		assertCounts(1, 1, 1);
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertCounts(1, 1, 1);
		assertEvents(1);
	}

	@Test
	void 확정이_먼저_롤백되면_대기하던_삭제후_재실행은_빈주를_확정한다() throws Exception {
		Long post = post(0);
		race(post, false, true);
		assertDeleted(post);
		assertCounts(0, 0, 0);
		assertEvents(0);
		assertThat(settlement.finalizePreviousWeek().ranks()).isZero();
		assertCounts(1, 0, 0);
		assertEvents(0);
	}

	@Test
	void 삭제글을_제외한_확정중_두번째_보상실패는_전체롤백하고_재시도시_각각_한번만_지급한다() {
		deletion.delete(author, post(0));
		Long first = post(1);
		Long second = post(2);
		jdbcTemplate.execute("ALTER TABLE generation_quota_grants ADD CONSTRAINT test_deletion_fail_second CHECK (rank_no <> 2) NOT VALID");
		try {
			assertThatThrownBy(settlement::finalizePreviousWeek).isInstanceOf(RuntimeException.class);
			assertCounts(0, 0, 0);
			assertEvents(0);
		} finally {
			jdbcTemplate.execute("ALTER TABLE generation_quota_grants DROP CONSTRAINT test_deletion_fail_second");
		}
		assertThat(settlement.finalizePreviousWeek().rewards()).isEqualTo(2);
		assertThat(settlement.finalizePreviousWeek().finalized()).isFalse();
		assertThat(jdbcTemplate.queryForList("SELECT shared_recipe_id FROM generation_quota_grants ORDER BY rank_no", Long.class))
				.containsExactly(first, second);
		assertThat(jdbcTemplate.queryForList("SELECT amount FROM generation_quota_grants ORDER BY rank_no", Integer.class))
				.containsExactly(10, 6);
		assertCounts(1, 2, 2);
		assertEvents(2);
	}

	private void race(Long post, boolean deleteFirst, boolean rollbackFirst) throws Exception {
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
				if (deleteFirst) {
					deletion.delete(author, post);
				} else {
					assertThat(settlement.finalizePreviousWeek().rewards()).isEqualTo(1);
				}
				locked.countDown();
				awaitLatch(release);
				if (rollbackFirst) {
					tx.setRollbackOnly();
				}
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				var second = pool.submit(() -> {
					if (deleteFirst) {
						settlement.finalizePreviousWeek();
					} else {
						deletion.delete(author, post);
					}
				});
				// 실제 DB 잠금 대기를 확인한다. 스레드가 아직 시작하지 않은 경우를 성공으로 보지 않는다.
				await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
						assertThat(jdbcTemplate.queryForObject("""
								SELECT COUNT(*) FROM pg_locks WHERE locktype='advisory'
								AND classid=230 AND objid=1 AND objsubid=2 AND NOT granted
								""", Integer.class)).isEqualTo(1));
				assertThat(second.isDone()).isFalse();
				// 삭제가 먼저면 대기 중인 확정이 주 완료 기록조차 선점하지 않아야 한다.
				assertCounts(0, 0, 0);
				if (deleteFirst) {
					// 미커밋 INSERT는 COUNT로 보이지 않는다. 대기 세션의 INSERT용 테이블 잠금도 없어야 한다.
					assertThat(jdbcTemplate.queryForObject("""
							SELECT COUNT(*) FROM pg_locks held JOIN pg_locks waiting ON waiting.pid=held.pid
							WHERE waiting.locktype='advisory' AND waiting.classid=230
							AND waiting.objid=1 AND waiting.objsubid=2 AND NOT waiting.granted
							AND held.relation='shared_recipe_weekly_settlements'::regclass
							AND held.mode='RowExclusiveLock' AND held.granted
							""", Integer.class)).isZero();
				}
				release.countDown();
				first.get(10, TimeUnit.SECONDS);
				second.get(10, TimeUnit.SECONDS);
			} finally {
				release.countDown();
			}
		}
	}

	private void awaitLatch(CountDownLatch latch) {
		try {
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		} catch (InterruptedException failure) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(failure);
		}
	}

	private void assertCounts(int weeks, int ranks, int grants) {
		List<String> tables = List.of("shared_recipe_weekly_settlements", "shared_recipe_weekly_ranks", "generation_quota_grants");
		assertThat(tables.stream().map(table -> jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).toList())
				.containsExactly(weeks, ranks, grants);
	}

	private void assertEvents(int expected) {
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name='weekly_reward_granted'", Integer.class))
				.isEqualTo(expected);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Integer.class)).isEqualTo(expected);
	}

	private void assertDeleted(Long post) {
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM shared_recipes WHERE id=?", String.class, post))
				.isEqualTo("DELETED");
	}

	private Long post(int order) {
		LocalDateTime created = boundary.minusDays(6).plusMinutes(order);
		Long post = jdbcTemplate.queryForObject("""
				INSERT INTO shared_recipes (recipe_id, title, image_key, status, created_at, updated_at)
				VALUES (?, '삭제와 확정', ?, 'VISIBLE', ?, ?) RETURNING id
				""", Long.class, createRecipe(author), "settlement-deletion-" + order + ".jpg", created, created);
		save(post, createUser(), boundary.minusHours(1));
		return post;
	}

	private void save(Long post, Long reader, LocalDateTime savedAt) {
		jdbcTemplate.update("""
				INSERT INTO saved_recipes (user_id, recipe_id, shared_recipe_id, saved_at)
				SELECT ?, recipe_id, id, ? FROM shared_recipes WHERE id=?
				""", reader, savedAt, post);
	}
}
