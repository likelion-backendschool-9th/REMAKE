package com.remake.quota;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

// API 27 남은 생성 횟수. 기본 시각 2026-03-04 10:00(서울)
class UsageQuotaApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/usage-quotas";

	private MockHttpServletRequestBuilder as(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private void used(Long userId, String type, LocalDateTime at) {
		jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, ?, ?)", userId, type, at);
	}

	@Test
	void 로그인하지_않으면_401이다() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

	@Test
	void 일반_회원은_레시피_하루_2회와_식단표_한_달_1회_5일이다() throws Exception {
		Long userId = createUser();

		mockMvc.perform(as(userId, get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recipe.limit").value(2))
				.andExpect(jsonPath("$.recipe.used").value(0))
				.andExpect(jsonPath("$.recipe.bonusRemaining").value(0))
				.andExpect(jsonPath("$.recipe.remaining").value(2))
				.andExpect(jsonPath("$.mealPlan.limit").value(1))
				.andExpect(jsonPath("$.mealPlan.used").value(0))
				.andExpect(jsonPath("$.mealPlan.remaining").value(1))
				.andExpect(jsonPath("$.mealPlan.maxDays").value(5));
	}

	@Test
	void 구독_회원은_레시피_하루_4회와_식단표_한_달_4회_7일이다() throws Exception {
		Long userId = createUser();
		jdbcTemplate.update("UPDATE users SET plan = 'PREMIUM' WHERE id = ?", userId);

		mockMvc.perform(as(userId, get(URL)))
				.andExpect(jsonPath("$.recipe.limit").value(4))
				.andExpect(jsonPath("$.recipe.remaining").value(4))
				.andExpect(jsonPath("$.mealPlan.limit").value(4))
				.andExpect(jsonPath("$.mealPlan.maxDays").value(7));
	}

	@Test
	void 레시피는_오늘_한국_시간_0시부터_식단표는_이번_달_1일부터_센다() throws Exception {
		Long userId = createUser();
		used(userId, "RECIPE", LocalDateTime.of(2026, 3, 3, 23, 59, 59));
		used(userId, "RECIPE", LocalDateTime.of(2026, 3, 4, 0, 0));
		used(userId, "MEAL_PLAN", LocalDateTime.of(2026, 2, 28, 23, 59));
		used(userId, "MEAL_PLAN", LocalDateTime.of(2026, 3, 1, 0, 0));

		mockMvc.perform(as(userId, get(URL)))
				.andExpect(jsonPath("$.recipe.used").value(1))
				.andExpect(jsonPath("$.recipe.remaining").value(1))
				.andExpect(jsonPath("$.mealPlan.used").value(1))
				.andExpect(jsonPath("$.mealPlan.remaining").value(0));
	}

	@Test
	void 다_쓰면_남은_횟수는_0이고_음수가_되지_않는다() throws Exception {
		Long userId = createUser();
		for (int i = 0; i < 3; i++) {
			used(userId, "RECIPE", LocalDateTime.of(2026, 3, 4, 9, i));
		}

		mockMvc.perform(as(userId, get(URL)))
				.andExpect(jsonPath("$.recipe.used").value(3))
				.andExpect(jsonPath("$.recipe.remaining").value(0));
	}

	@Test
	void 다른_회원의_사용은_세지_않는다() throws Exception {
		Long userId = createUser();
		used(createUser(), "RECIPE", LocalDateTime.of(2026, 3, 4, 9, 0));

		mockMvc.perform(as(userId, get(URL)))
				.andExpect(jsonPath("$.recipe.used").value(0));
	}
}
