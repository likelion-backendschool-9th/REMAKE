package com.remake.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.AllergenCode;

// API 38 식단표 생성: 접수(202·거절)와 서버 작업(완성·실패). 기본 시각 2026-03-04 10:00(서울), 일반 회원은 월 1회·최대 5일.
class MealPlanGenerateApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/meal-plans/generate";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	// ---- 요청·준비 ----

	private ResultActions generate(Long userId, String body) throws Exception {
		return mockMvc.perform(post(URL)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private static String body(LocalDate startDate, String mealTypes, boolean overwrite) {
		return """
				{"startDate":"%s","purpose":"HIGH_PROTEIN","mealTypes":%s,"servings":2,"overwrite":%s}
				""".formatted(startDate, mealTypes, overwrite);
	}

	/** 냉장고 재료(채소, 그램 단위, 기한 넉넉함). */
	private Long fridgeGrams(Long userId, String name, int grams) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, ?, ?, 'G', NULL, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId("VEGETABLE"), name, TODAY.plusDays(30), grams, grams, now, now);
	}

	/** 이미 완성된 식단표 하나(칸은 따로 넣는다). */
	private Long completedPlan(Long userId, LocalDate startDate, int days) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, purpose, meal_types, servings, generated_days, status,
				                        created_at, updated_at)
				VALUES (?, ?, 'BALANCED', 'DINNER', 1, ?, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, userId, startDate, days, now, now);
	}

	/** 칸 하나. 레시피는 냉장고 재료 하나를 grams만큼 쓴다. */
	private Long slot(Long userId, Long mealPlanId, LocalDate date, Long fridgeId, int grams, boolean cooked) {
		Long recipeId = createRecipe(userId);
		jdbcTemplate.update("""
				INSERT INTO recipe_ingredients (recipe_id, ingredient_id, name, amount, unit, sort_order, created_at)
				VALUES (?, ?, '대파', ?, 'G', 0, ?)
				""", recipeId, fridgeId, grams, LocalDateTime.now(clock));
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, cooked_at, created_at,
				                             updated_at)
				VALUES (?, ?, 'DINNER', ?, ?, ?, ?)
				RETURNING id
				""", Long.class, mealPlanId, date, recipeId, cooked ? now : null, now, now);
	}

	/** AI 가짜: 요청한 칸마다 냉장고 재료 gramsEach와 냉장고에 없는 간장 1큰술을 쓰는 레시피. */
	private void aiUses(Long fridgeId, int gramsEach) {
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			MealPlanAiRequest request = invocation.getArgument(0);
			List<GeneratedMealSlot> slots = new ArrayList<>();
			for (LocalDate date : request.schedule().dates()) {
				for (MealType mealType : request.schedule().mealTypesOn(date)) {
					slots.add(new GeneratedMealSlot(date, mealType, "대파 볶음 " + date, 15, null, List.of(
							new SlotIngredient(fridgeId, "대파", BigDecimal.valueOf(gramsEach), RecipeUnit.G),
							new SlotIngredient(null, "간장", BigDecimal.ONE, RecipeUnit.TBSP)),
							List.of("대파를 썬다", "볶는다")));
				}
			}
			return slots;
		});
	}

	private Map<String, Object> mealPlan(Long id) {
		return jdbcTemplate.queryForMap("SELECT status, generated_days FROM meal_plans WHERE id = ?", id);
	}

	private Long newestPlanId(Long userId) {
		return jdbcTemplate.queryForObject("SELECT MAX(id) FROM meal_plans WHERE user_id = ?", Long.class, userId);
	}

	private List<LocalDate> slotDates(Long mealPlanId) {
		return jdbcTemplate.queryForList("SELECT slot_date FROM meal_plan_slots WHERE meal_plan_id = ? ORDER BY slot_date",
				LocalDate.class, mealPlanId);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private String eventProperty(String eventName, String property) {
		awaitEventWrites();
		return jdbcTemplate.queryForObject("SELECT properties->>? FROM events WHERE event_name = ?", String.class,
				property, eventName);
	}

	// ---- 접수 ----

	@Test
	void 접수하면_202와_GENERATING을_바로_돌려준다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.mealPlanId").isNumber())
				.andExpect(jsonPath("$.status").value("GENERATING"));
	}

	@Test
	void 접수하면_고른_식단_목적을_다음_기본값으로_기억하고_거절된_요청은_바꾸지_않는다() throws Exception {
		// 냉장고가 비어 생성은 재료 부족으로 실패하지만(차감 없음) 고른 목적은 기억한다
		Long userId = createUser();

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();
		assertThat(lastMealPurpose(userId)).isEqualTo("HIGH_PROTEIN");

		generate(userId, purposeBody(TODAY.minusDays(1), "DIET")).andExpect(status().isBadRequest());
		assertThat(lastMealPurpose(userId)).isEqualTo("HIGH_PROTEIN");

		generate(userId, purposeBody(TODAY, "DIET")).andExpect(status().isAccepted());
		awaitMealPlanJobs();
		mockMvc.perform(get("/api/v1/user/profile").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lastMealPurpose").value("DIET"));
	}

	private String lastMealPurpose(Long userId) {
		return jdbcTemplate.queryForObject("SELECT last_meal_purpose FROM users WHERE id = ?", String.class, userId);
	}

	private static String purposeBody(LocalDate startDate, String purpose) {
		return """
				{"startDate":"%s","purpose":"%s","mealTypes":["DINNER"],"servings":2}
				""".formatted(startDate, purpose);
	}

	@Test
	void 지난_시작일은_PAST_DATE_NOT_ALLOWED다() throws Exception {
		Long userId = createUser();

		generate(userId, body(TODAY.minusDays(1), "[\"DINNER\"]", false))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_DATE_NOT_ALLOWED"));
		assertThat(count("SELECT COUNT(*) FROM meal_plans")).isZero();
	}

	@Test
	void 끼니가_없거나_중복이면_INVALID_INPUT이다() throws Exception {
		Long userId = createUser();

		generate(userId, body(TODAY, "[]", false))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		generate(userId, body(TODAY, "[\"DINNER\",\"DINNER\"]", false))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 식사_인원이_1부터_50_밖이면_INVALID_INPUT이다() throws Exception {
		Long userId = createUser();

		for (int servings : new int[] {0, 51}) {
			generate(userId, """
					{"startDate":"%s","purpose":"BALANCED","mealTypes":["DINNER"],"servings":%d}
					""".formatted(TODAY, servings))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
					.andExpect(jsonPath("$.fieldErrors[0].field").value("servings"));
		}
		assertThat(count("SELECT COUNT(*) FROM meal_plans WHERE user_id = ?", userId)).isZero();
	}

	@Test
	void 생성_중인_식단표가_있으면_MEAL_PLAN_IN_PROGRESS다() throws Exception {
		Long userId = createUser();
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, status, created_at, updated_at)
				VALUES (?, ?, 'DINNER', 1, 'GENERATING', ?, ?)
				""", userId, TODAY, now, now);

		generate(userId, body(TODAY.plusDays(10), "[\"DINNER\"]", false))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_IN_PROGRESS"));
		assertThat(count("SELECT COUNT(*) FROM meal_plans WHERE user_id = ?", userId)).isEqualTo(1);
	}

	@Test
	void 이번_달_횟수를_다_쓰면_429이고_quota_exceeded를_남긴다() throws Exception {
		Long userId = createUser();
		jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'MEAL_PLAN', ?)", userId,
				TODAY.withDayOfMonth(1).atStartOfDay());

		generate(userId, body(TODAY, "[\"DINNER\"]", false))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_LIMIT_EXCEEDED"));

		assertThat(count("SELECT COUNT(*) FROM meal_plans WHERE user_id = ?", userId)).isZero();
		assertThat(eventProperty("quota_exceeded", "limit_feature")).isEqualTo("MEAL_PLAN");
		assertThat(eventProperty("quota_exceeded", "used_count")).isEqualTo("1");
	}

	@Test
	void 지난달_기록은_이번_달_횟수에_세지_않는다() throws Exception {
		Long userId = createUser();
		jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'MEAL_PLAN', ?)", userId,
				TODAY.withDayOfMonth(1).atStartOfDay().minusSeconds(1));
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
	}

	@Test
	void 기존_식단과_겹치면_덮어쓰기_없이는_MEAL_PLAN_OVERLAP과_겹치는_날짜를_돌려준다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY.plusDays(1), 1);
		slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);

		generate(userId, body(TODAY, "[\"BREAKFAST\"]", false))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_OVERLAP"))
				.andExpect(jsonPath("$.details.overlappingDates[0]").value("2026-03-05"));
		assertThat(count("SELECT COUNT(*) FROM meal_plans WHERE user_id = ?", userId)).isEqualTo(1);
	}

	// ---- 서버 작업 ----

	@Test
	void 작업이_성공하면_칸_레시피_월_차감_완성_알림_이벤트가_남는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
		assertThat(slotDates(planId)).containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3),
				TODAY.plusDays(4));
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ? AND source = 'MEAL_PLAN' AND servings = 2",
				userId)).isEqualTo(5);
		assertThat(count("""
				SELECT COUNT(*) FROM recipe_ingredients i JOIN recipes r ON r.id = i.recipe_id
				WHERE r.user_id = ? AND i.ingredient_id = ?
				""", userId, fridgeId)).isEqualTo(5);
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ? AND type = 'MEAL_PLAN'", userId))
				.isEqualTo(1);
		assertThat(count("""
				SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_COMPLETED' AND meal_plan_id = ?
				""", userId, planId)).isEqualTo(1);
		assertThat(eventProperty("meal_plan_generated", "generated_days")).isEqualTo("5");
		assertThat(eventProperty("meal_plan_generated", "shortage_count")).isEqualTo("5");
		assertThat(eventProperty("meal_plan_generated", "remaining_quota")).isEqualTo("0");
		assertThat(eventProperty("meal_plan_generated", "cart_added_count")).isEqualTo("0");
		assertThat(eventProperty("meal_plan_generated", "was_overwritten")).isEqualTo("false");
	}

	@Test
	void 냉장고_재료가_모자라면_만들_수_있는_날까지만_만든다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 250);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 2);
		assertThat(slotDates(planId)).containsExactly(TODAY, TODAY.plusDays(1));
	}

	@Test
	void 다른_식단표에_배정된_양은_빼고_AI에_보낸다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long otherPlan = completedPlan(userId, TODAY.plusDays(10), 1);
		slot(userId, otherPlan, TODAY.plusDays(10), fridgeId, 900, false);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		ArgumentCaptor<MealPlanAiRequest> request = ArgumentCaptor.forClass(MealPlanAiRequest.class);
		verify(mealPlanAiAdapter).generate(request.capture());
		assertThat(request.getValue().ingredients()).singleElement()
				.extracting(FridgeStock::amount)
				.satisfies(amount -> assertThat(amount).isEqualByComparingTo("100"));
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("generated_days", 1);
		assertThat(request.getValue().excludedAllergenCodes()).isEmpty();
	}

	@Test
	void 설정의_제외_재료를_AI에_쓰지_말_재료로_보낸다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		for (AllergenCode code : List.of(AllergenCode.MILK, AllergenCode.EGG)) {
			jdbcTemplate.update("INSERT INTO user_excluded_ingredients (user_id, allergen_code) VALUES (?, ?)", userId,
					code.name());
		}
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		ArgumentCaptor<MealPlanAiRequest> request = ArgumentCaptor.forClass(MealPlanAiRequest.class);
		verify(mealPlanAiAdapter).generate(request.capture());
		assertThat(request.getValue().excludedAllergenCodes())
				.containsExactlyInAnyOrder(AllergenCode.MILK, AllergenCode.EGG);
	}

	@Test
	void 냉장고_재료가_없으면_AI를_부르지_않고_재료_부족으로_실패한다() throws Exception {
		Long userId = createUser();

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "FAILED");
		verify(mealPlanAiAdapter, never()).generate(any());
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT message FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_FAILED'", String.class,
				userId)).contains("냉장고 재료가 부족해");
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("MEAL_PLAN_NO_INGREDIENTS");
	}

	@Test
	void AI가_하루도_못_채우면_재료_부족으로_실패한다() throws Exception {
		Long userId = createUser();
		fridgeGrams(userId, "대파", 1000);
		// AI 가짜를 stub하지 않으면 빈 목록을 돌려준다

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("MEAL_PLAN_NO_INGREDIENTS");
	}

	@Test
	void 덮어쓰면_요청_기간의_기존_칸을_바꾸고_오늘_요리한_칸과_범위_밖_칸은_남긴다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY, 7);
		Long cookedToday = slot(userId, oldPlan, TODAY, fridgeId, 100, true);
		Long tomorrow = slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);
		Long outside = slot(userId, oldPlan, TODAY.plusDays(6), fridgeId, 100, false);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long newPlan = newestPlanId(userId);
		assertThat(mealPlan(newPlan)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
		// 오늘 저녁은 이미 요리해서 새로 채우지 않는다
		assertThat(slotDates(newPlan)).containsExactly(TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3),
				TODAY.plusDays(4));
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", cookedToday)).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", tomorrow)).isZero();
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", outside)).isEqualTo(1);
		assertThat(eventProperty("meal_plan_generated", "was_overwritten")).isEqualTo("true");
	}

	@Test
	void 덮어쓰면_기존_칸의_재료를_처음부터_쓰고_못_만든_뒤쪽_날은_빈_날이_된다() throws Exception {
		// 대파 1000g을 기존 식단 5일이 하루 200g씩 모두 잡아 둠. 같은 5일을 덮어쓰고 새 레시피는 하루 300g.
		// 기존 칸의 1000g을 처음부터 돌려받아 3일치(900g)를 만들고, 4·5일의 기존 칸은 지워 빈 날이 된다.
		// (하루씩 돌려받던 이전 규칙이면 첫날 200g < 300g이라 하루도 못 만들었다, #78 11-24)
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY, 5);
		for (int day = 0; day < 5; day++) {
			slot(userId, oldPlan, TODAY.plusDays(day), fridgeId, 200, false);
		}
		aiUses(fridgeId, 300);

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long newPlan = newestPlanId(userId);
		assertThat(mealPlan(newPlan)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 3);
		assertThat(slotDates(newPlan)).containsExactly(TODAY, TODAY.plusDays(1), TODAY.plusDays(2));
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE meal_plan_id = ?", oldPlan)).isZero();
		assertThat(eventProperty("meal_plan_generated", "was_overwritten")).isEqualTo("true");
	}

	@Test
	void 덮어쓰기가_실패하면_기존_식단과_횟수가_그대로다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY.plusDays(1), 1);
		Long oldSlot = slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);
		given(mealPlanAiAdapter.generate(any())).willThrow(new BusinessException(ErrorCode.AI_TIMEOUT));

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", oldSlot)).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_FAILED'",
				userId)).isEqualTo(1);
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("AI_TIMEOUT");
	}

	@Test
	void 알림을_끈_회원도_완성_알림을_받는다() throws Exception {
		Long userId = createUser();
		jdbcTemplate.update("UPDATE users SET notification_enabled = false WHERE id = ?", userId);
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(count("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_COMPLETED'",
				userId)).isEqualTo(1);
	}

	@Test
	void 완성된_뒤에는_다시_만들_수_있지만_이번_달_횟수는_남지_않는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		generate(userId, body(TODAY.plusDays(20), "[\"DINNER\"]", false))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_LIMIT_EXCEEDED"));
	}
}
