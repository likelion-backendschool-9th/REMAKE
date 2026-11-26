package com.remake.mealplan;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

// API 39 식단표 월 달력. 기간은 완성된 식단표만(#144 확정), 날짜는 칸이 있는 날.
// hasAnyMealPlan은 완성된 식단표가 하나라도 있는지(달 상관없이, 빈 화면 조건).
class MealPlanCalendarApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/meal-plans/calendar";

	private ResultActions calendar(Long userId, String month) throws Exception {
		return mockMvc.perform(get(URL).param("month", month)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId));
	}

	private Long plan(Long userId, LocalDate startDate, Integer days, String status) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, generated_days, status, created_at,
				                        updated_at)
				VALUES (?, ?, 'DINNER', 1, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, userId, startDate, days, status, now, now);
	}

	private void slot(Long userId, Long mealPlanId, LocalDate date) {
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, created_at, updated_at)
				VALUES (?, ?, 'DINNER', ?, ?, ?)
				""", mealPlanId, date, createRecipe(userId), now, now);
	}

	@Test
	void 로그인_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL).param("month", "2026-03")).andExpect(status().isUnauthorized());
	}

	@Test
	void 식단표를_만든_적이_없으면_빈_목록이고_hasAnyMealPlan이_false다() throws Exception {
		Long userId = createUser();

		calendar(userId, "2026-03")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.month").value("2026-03"))
				.andExpect(jsonPath("$.hasAnyMealPlan").value(false))
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.datesWithMeals", empty()));
	}

	@Test
	void 그_달에_걸친_완성_식단표_기간과_칸이_있는_날을_돌려준다() throws Exception {
		Long userId = createUser();
		Long other = createUser();
		// 지난달 말에 시작해 이번 달로 이어지는 식단표(2/26~3/2)
		Long crossing = plan(userId, LocalDate.of(2026, 2, 26), 5, "COMPLETED");
		slot(userId, crossing, LocalDate.of(2026, 2, 27));
		slot(userId, crossing, LocalDate.of(2026, 3, 2));
		Long march = plan(userId, LocalDate.of(2026, 3, 10), 3, "COMPLETED");
		slot(userId, march, LocalDate.of(2026, 3, 10));
		slot(userId, march, LocalDate.of(2026, 3, 12));
		// 넣지 않는 것: 지난달에 끝난 식단표, 생성 중·실패, 다른 회원
		plan(userId, LocalDate.of(2026, 2, 20), 3, "COMPLETED");
		plan(userId, LocalDate.of(2026, 3, 20), null, "FAILED");
		plan(userId, LocalDate.of(2026, 3, 25), null, "GENERATING");
		Long others = plan(other, LocalDate.of(2026, 3, 5), 2, "COMPLETED");
		slot(other, others, LocalDate.of(2026, 3, 5));

		calendar(userId, "2026-03")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasAnyMealPlan").value(true))
				.andExpect(jsonPath("$.periods[*].mealPlanId").value(contains(crossing.intValue(), march.intValue())))
				.andExpect(jsonPath("$.periods[0].startDate").value("2026-02-26"))
				.andExpect(jsonPath("$.periods[0].endDate").value("2026-03-02"))
				.andExpect(jsonPath("$.periods[1].startDate").value("2026-03-10"))
				.andExpect(jsonPath("$.periods[1].endDate").value("2026-03-12"))
				.andExpect(jsonPath("$.datesWithMeals").value(contains("2026-03-02", "2026-03-10", "2026-03-12")));
	}

	@Test
	void 다른_달에만_완성_식단표가_있으면_빈_달이어도_hasAnyMealPlan이_true다() throws Exception {
		Long userId = createUser();
		Long january = plan(userId, LocalDate.of(2026, 1, 10), 3, "COMPLETED");
		slot(userId, january, LocalDate.of(2026, 1, 10));

		calendar(userId, "2026-03")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasAnyMealPlan").value(true))
				.andExpect(jsonPath("$.periods", empty()))
				.andExpect(jsonPath("$.datesWithMeals", empty()));
	}

	@Test
	void 생성_중이거나_실패한_식단표만_있으면_hasAnyMealPlan이_false다() throws Exception {
		Long userId = createUser();
		plan(userId, LocalDate.of(2026, 3, 5), null, "FAILED");
		plan(userId, LocalDate.of(2026, 3, 10), null, "GENERATING");
		// 다른 회원의 완성 식단표는 세지 않는다
		plan(createUser(), LocalDate.of(2026, 3, 1), 2, "COMPLETED");

		calendar(userId, "2026-03")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.hasAnyMealPlan").value(false))
				.andExpect(jsonPath("$.periods", empty()));
	}

	@Test
	void 지난_달도_조회한다() throws Exception {
		Long userId = createUser();
		Long crossing = plan(userId, LocalDate.of(2026, 2, 26), 5, "COMPLETED");
		slot(userId, crossing, LocalDate.of(2026, 2, 27));
		slot(userId, crossing, LocalDate.of(2026, 3, 2));

		calendar(userId, "2026-02")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.periods[0].mealPlanId").value(crossing.intValue()))
				.andExpect(jsonPath("$.datesWithMeals").value(contains("2026-02-27")));
	}

	@Test
	void month_형식이_틀리거나_없으면_INVALID_INPUT이다() throws Exception {
		Long userId = createUser();

		calendar(userId, "2026-13")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		calendar(userId, "202603")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(get(URL).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}
}
