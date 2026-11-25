package com.remake.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.notification.application.port.in.CreateMealPlanNotificationUseCase;
import com.remake.notification.domain.MealPlanFailReason;
import com.remake.support.IntegrationTestSupport;

class MealPlanNotificationTest extends IntegrationTestSupport {

	private static final LocalDate START = LocalDate.of(2026, 11, 24);

	private Long me;

	@Autowired
	CreateMealPlanNotificationUseCase createMealPlanNotificationUseCase;

	@BeforeEach
	void setUp() {
		me = createUser();
		jdbcTemplate.update("UPDATE users SET notification_enabled = false WHERE id = ?", me);
	}

	private MockHttpServletRequestBuilder as(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private Long insertMealPlan() {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, purpose, meal_types, servings, generated_days, status,
				                        created_at, updated_at)
				VALUES (?, ?, 'BALANCED', 'DINNER', 1, 5, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, me, START, now, now);
	}

	@Test
	void 알림을_꺼도_식단표_완성_알림을_만들고_목록에_mealPlanId가_나온다() throws Exception {
		Long mealPlanId = insertMealPlan();

		createMealPlanNotificationUseCase.notifyMealPlanCompleted(me, mealPlanId, START, 5, 5, 0);
		awaitEventWrites();

		mockMvc.perform(as(me, get("/api/v1/user/notifications")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].type").value("MEAL_PLAN_COMPLETED"))
				.andExpect(jsonPath("$.content[0].message").value("11월 24일부터 5일치 식단표가 완성됐어요"))
				.andExpect(jsonPath("$.content[0].mealPlanId").value(mealPlanId.intValue()));

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, me);
		assertThat(sent).isEqualTo(1);
		assertSentProperties("meal_plan_completed");
	}

	private void assertSentProperties(String notificationType) {
		Map<String, Object> properties = jdbcTemplate.queryForMap("""
				SELECT properties ->> 'notification_type' AS notification_type,
				       properties ->> 'batch_count' AS batch_count
				FROM events
				WHERE event_name = 'notification_sent' AND user_id = ?
				""", me);
		assertThat(properties)
				.containsEntry("notification_type", notificationType)
				.containsEntry("batch_count", "1");

		List<String> keys = jdbcTemplate.queryForList(
				"SELECT DISTINCT jsonb_object_keys(properties) FROM events WHERE event_name = 'notification_sent'",
				String.class);
		assertThat(keys).containsExactlyInAnyOrder("notification_type", "batch_count");
	}

	@Test
	void 식단표_실패_알림은_mealPlanId가_없고_사유_문구를_쓴다() throws Exception {
		createMealPlanNotificationUseCase.notifyMealPlanFailed(me, MealPlanFailReason.NO_INGREDIENTS);
		awaitEventWrites();

		mockMvc.perform(as(me, get("/api/v1/user/notifications")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].type").value("MEAL_PLAN_FAILED"))
				.andExpect(jsonPath("$.content[0].message")
						.value("냉장고 재료가 부족해 식단표를 만들지 못했어요. 생성 횟수는 차감되지 않았어요"))
				.andExpect(jsonPath("$.content[0].mealPlanId").value(org.hamcrest.Matchers.nullValue()));

		Integer sent = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'notification_sent' AND user_id = ?",
				Integer.class, me);
		assertThat(sent).isEqualTo(1);
		assertSentProperties("meal_plan_failed");
	}
}
