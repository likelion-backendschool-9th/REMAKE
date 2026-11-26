package com.remake.mealplan.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.mealplan.application.port.in.FailAbandonedMealPlansUseCase;
import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.notification.domain.MealPlanFailReason;
import com.remake.support.IntegrationTestSupport;

/** 끊긴 식단표 생성 작업 정리. 스케줄러는 테스트에서 꺼져 있어 UseCase를 직접 부른다. */
class MealPlanAbandonedJobServiceTest extends IntegrationTestSupport {

	@Autowired
	private FailAbandonedMealPlansUseCase failAbandonedMealPlansUseCase;

	@Autowired
	private MealPlanResultWriter resultWriter;

	/** 접수 시각이 acceptedAt인 GENERATING 식단표. */
	private Long generatingPlan(Long userId, LocalDateTime acceptedAt) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, status, created_at, updated_at)
				VALUES (?, ?, 'DINNER', 1, 'GENERATING', ?, ?)
				RETURNING id
				""", Long.class, userId, LocalDate.now(clock), acceptedAt, acceptedAt);
	}

	private String planStatus(Long mealPlanId) {
		return jdbcTemplate.queryForObject("SELECT status FROM meal_plans WHERE id = ?", String.class, mealPlanId);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private int failedNotifications(Long userId) {
		return count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_FAILED'", userId);
	}

	@Test
	void 접수_뒤_10분이_지나도_생성_중이면_실패로_끝내고_실패_알림을_보낸다() {
		Long userId = createUser();
		LocalDateTime acceptedAt = LocalDateTime.now(clock).minusMinutes(11);
		Long planId = generatingPlan(userId, acceptedAt);

		assertThat(failAbandonedMealPlansUseCase.failAbandonedMealPlans()).isEqualTo(1);

		assertThat(planStatus(planId)).isEqualTo("FAILED");
		assertThat(failedNotifications(userId)).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForMap("""
				SELECT properties->>'error_code' AS code, (properties->>'duration_ms')::bigint AS duration
				FROM events WHERE event_name = 'meal_plan_generate_failed' AND user_id = ?
				""", userId))
				.containsEntry("code", "AI_TIMEOUT")
				.containsEntry("duration", 11 * 60 * 1000L);
	}

	@Test
	void 접수_뒤_10분이_안_된_생성_중과_이미_끝난_식단표는_그대로_둔다() {
		Long recentUser = createUser();
		Long recent = generatingPlan(recentUser, LocalDateTime.now(clock).minusMinutes(9));
		Long doneUser = createUser();
		LocalDateTime longAgo = LocalDateTime.now(clock).minusHours(1);
		Long completed = jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, generated_days, status, created_at,
				                        updated_at)
				VALUES (?, ?, 'DINNER', 1, 1, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, doneUser, LocalDate.now(clock), longAgo, longAgo);

		assertThat(failAbandonedMealPlansUseCase.failAbandonedMealPlans()).isZero();

		assertThat(planStatus(recent)).isEqualTo("GENERATING");
		assertThat(planStatus(completed)).isEqualTo("COMPLETED");
		assertThat(failedNotifications(recentUser) + failedNotifications(doneUser)).isZero();
	}

	@Test
	void 두_번_돌려도_알림은_한_번만_간다() {
		Long userId = createUser();
		generatingPlan(userId, LocalDateTime.now(clock).minusMinutes(30));

		assertThat(failAbandonedMealPlansUseCase.failAbandonedMealPlans()).isEqualTo(1);
		assertThat(failAbandonedMealPlansUseCase.failAbandonedMealPlans()).isZero();

		assertThat(failedNotifications(userId)).isEqualTo(1);
	}

	@Test
	void 정리된_뒤_늦게_끝난_작업의_결과는_저장하지_않는다() {
		Long userId = createUser();
		LocalDateTime acceptedAt = LocalDateTime.now(clock).minusMinutes(11);
		Long planId = generatingPlan(userId, acceptedAt);
		failAbandonedMealPlansUseCase.failAbandonedMealPlans();
		MealPlanGenerationJob lateJob = new MealPlanGenerationJob(planId, userId, 5, false,
				acceptedAt.atZone(clock.getZone()).toInstant());

		assertThatThrownBy(() -> resultWriter.complete(lateJob, List.of(), 1, 0, List.of(), List.of()))
				.isInstanceOf(IllegalStateException.class);

		assertThat(planStatus(planId)).isEqualTo("FAILED");
		assertThat(count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_COMPLETED'",
				userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
	}

	@Test
	void 정리된_뒤에는_같은_회원이_다시_만들_수_있다() throws Exception {
		Long userId = createUser();
		generatingPlan(userId, LocalDateTime.now(clock).minusMinutes(11));
		failAbandonedMealPlansUseCase.failAbandonedMealPlans();

		mockMvc.perform(post("/api/v1/user/meal-plans/generate")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"startDate":"%s","purpose":"BALANCED","mealTypes":["DINNER"],"servings":1}
						""".formatted(LocalDate.now(clock))))
				.andExpect(status().isAccepted());
		awaitMealPlanJobs();
	}

	@Test
	void 일반_실패_처리도_정리와_같은_기준으로_한_번만_끝낸다() {
		Long userId = createUser();
		Instant acceptedAt = Instant.now(clock);
		Long planId = generatingPlan(userId, LocalDateTime.now(clock));
		MealPlanGenerationJob job = new MealPlanGenerationJob(planId, userId, 5, false, acceptedAt);

		resultWriter.fail(job, MealPlanFailReason.GENERIC, "AI_UPSTREAM_ERROR");
		clock.advance(Duration.ofMinutes(11));

		assertThat(failAbandonedMealPlansUseCase.failAbandonedMealPlans()).isZero();
		assertThat(failedNotifications(userId)).isEqualTo(1);
	}
}
