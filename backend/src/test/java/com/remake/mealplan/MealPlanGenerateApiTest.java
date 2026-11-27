package com.remake.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.mealplan.application.port.in.MarkMealSlotCookedUseCase;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiBusyException;
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

	@Autowired
	private MarkMealSlotCookedUseCase markMealSlotCookedUseCase;

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
		return fridgeGrams(userId, name, grams, TODAY.plusDays(30));
	}

	private Long fridgeGrams(Long userId, String name, int grams, LocalDate expirationDate) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, ?, ?, 'G', NULL, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId("VEGETABLE"), name, expirationDate, grams, grams, now, now);
	}

	/** 냉장고 재료(카테고리·단위를 고름, 기한 넉넉함). */
	private Long fridgeItem(Long userId, String name, String categoryCode, int amount, String unit) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, ?, ?, ?, NULL, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId(categoryCode), name, TODAY.plusDays(30), amount, amount, unit, now,
				now);
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
		given(mealPlanAiAdapter.generate(any())).willAnswer(everySlotUses(fridgeId, gramsEach));
	}

	private static Answer<List<GeneratedMealSlot>> everySlotUses(Long fridgeId, int gramsEach) {
		return invocation -> {
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
		};
	}

	/** AI 가짜: 요청한 칸마다 냉장고 재료 gramsEach에 extras를 더 쓰는 레시피. */
	private void aiUsesWith(Long fridgeId, int gramsEach, List<SlotIngredient> extras) {
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			MealPlanAiRequest request = invocation.getArgument(0);
			List<GeneratedMealSlot> slots = new ArrayList<>();
			for (LocalDate date : request.schedule().dates()) {
				for (MealType mealType : request.schedule().mealTypesOn(date)) {
					List<SlotIngredient> ingredients = new ArrayList<>();
					ingredients.add(new SlotIngredient(fridgeId, "우리집 대파", BigDecimal.valueOf(gramsEach),
							RecipeUnit.G, IngredientCategoryCode.ETC));
					ingredients.addAll(extras);
					slots.add(new GeneratedMealSlot(date, mealType, "대파 볶음 " + date, 15, null, ingredients,
							List.of("대파를 썬다", "볶는다")));
				}
			}
			return slots;
		});
	}

	/** 장바구니 행 하나(식단표에서 담은 것). edited면 담은 뒤 수량을 고친 행(updated_at이 created_at보다 뒤). */
	private Long cartItem(Long userId, Long recipeId, Long slotId, String name, int amount, boolean edited) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, recipe_id, meal_plan_slot_id, category_id, name, amount, unit,
				                        created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, 'TBSP', ?, ?)
				RETURNING id
				""", Long.class, userId, recipeId, slotId, categoryId("SEASONING"), name, amount, now,
				edited ? now.plusMinutes(5) : now);
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

	private String completedMessage(Long planId) {
		return jdbcTemplate.queryForObject(
				"SELECT message FROM notifications WHERE type = 'MEAL_PLAN_COMPLETED' AND meal_plan_id = ?",
				String.class, planId);
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
		assertThat(eventProperty("quota_exceeded", "limit_feature")).isEqualTo("meal_plan");
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
		// 5일을 다 채워도 장바구니에 담았으면 담은 개수를 알린다(#91)
		assertThat(completedMessage(planId)).endsWith("부터 5일치 식단표가 완성됐어요. 부족한 재료 5개를 장바구니에 담았어요");
		assertThat(eventProperty("meal_plan_generated", "generated_days")).isEqualTo("5");
		assertThat(eventProperty("meal_plan_generated", "shortage_count")).isEqualTo("5");
		assertThat(eventProperty("meal_plan_generated", "remaining_quota")).isEqualTo("0");
		// 칸마다 레시피가 따로라 간장이 레시피마다 한 행씩 담긴다(#91)
		assertThat(eventProperty("meal_plan_generated", "cart_added_count")).isEqualTo("5");
		assertThat(count("""
				SELECT COUNT(*) FROM cart_items c JOIN meal_plan_slots s ON s.id = c.meal_plan_slot_id
				WHERE c.user_id = ? AND s.meal_plan_id = ? AND c.recipe_id = s.recipe_id AND c.name = '간장'
				""", userId, planId)).isEqualTo(5);
		assertThat(eventProperty("meal_plan_generated", "was_overwritten")).isEqualTo("false");
	}

	@Test
	void 냉장고_재료가_모자라면_만들_수_있는_날까지만_만들고_완성_알림에_부족_문장을_붙인다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 250);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 2);
		assertThat(slotDates(planId)).containsExactly(TODAY, TODAY.plusDays(1));
		assertThat(completedMessage(planId)).endsWith(
				"부터 2일치 식단표가 완성됐어요. 재료가 부족해 2일치만 만들었어요. 부족한 재료 2개를 장바구니에 담았어요");
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
	void AI가_첫날부터_냉장고_재료를_넘게_쓰면_다시_부르지_않고_재료_부족으로_실패한다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		aiUses(fridgeId, 2000);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(1)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("MEAL_PLAN_NO_INGREDIENTS");
	}

	@Test
	void AI가_칸을_빠뜨리면_한_번_더_불러_두_번째_응답으로_만든다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willReturn(List.of()).willAnswer(everySlotUses(fridgeId, 100));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(2)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
	}

	@Test
	void AI가_두_번_다_칸이_망가지면_AI_INVALID_RESPONSE로_실패하고_횟수는_그대로다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		// 칸은 다 있지만 첫날 레시피의 단계가 비어 있다(칸 하나만 망가져도 응답 전체가 실패)
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			List<GeneratedMealSlot> slots = new ArrayList<>(everySlotUses(fridgeId, 100).answer(invocation));
			GeneratedMealSlot first = slots.getFirst();
			slots.set(0, new GeneratedMealSlot(first.date(), first.mealType(), first.title(), first.cookMinutes(),
					first.tips(), first.ingredients(), List.of()));
			return slots;
		});

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(2)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ?", userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("AI_INVALID_RESPONSE");
	}

	@Test
	void AI가_재료가_떨어진_칸을_못_만듦으로_보내면_다시_부르지_않고_그_앞날까지_만든다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			List<GeneratedMealSlot> slots = new ArrayList<>(everySlotUses(fridgeId, 100).answer(invocation));
			for (int i = 2; i < slots.size(); i++) {
				slots.set(i, GeneratedMealSlot.skipped(slots.get(i).date(), slots.get(i).mealType()));
			}
			return slots;
		});

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(1)).generate(any());
		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 2);
		assertThat(slotDates(planId)).containsExactly(TODAY, TODAY.plusDays(1));
	}

	@Test
	void AI가_과부하로_거절하면_더_불러_만든다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willThrow(new MealPlanAiBusyException(), new MealPlanAiBusyException(),
				new MealPlanAiBusyException()).willAnswer(everySlotUses(fridgeId, 100));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(4)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "COMPLETED");
	}

	@Test
	void AI가_10번_모두_과부하면_AI_UPSTREAM_ERROR로_실패한다() throws Exception {
		Long userId = createUser();
		fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willThrow(new MealPlanAiBusyException());

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(10)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("AI_UPSTREAM_ERROR");
	}

	@Test
	void 시작일_전에_소비기한이_끝나는_재료는_AI에_보내지_않는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		fridgeGrams(userId, "버섯", 200, TODAY.plusDays(1));
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY.plusDays(2), "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		ArgumentCaptor<MealPlanAiRequest> request = ArgumentCaptor.forClass(MealPlanAiRequest.class);
		verify(mealPlanAiAdapter).generate(request.capture());
		assertThat(request.getValue().ingredients()).extracting(FridgeStock::name).containsExactly("대파");
	}

	// ---- 조합 기준(#288 F) ----

	@Test
	void 식약처_요리에_안_나오는_냉장고_재료와_사전에_없는_음료는_AI에_보내지_않는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		fridgeItem(userId, "콜라", "BEVERAGE", 1500, "ML");
		fridgeItem(userId, "밀크 초콜릿", "ETC", 200, "G");
		fridgeItem(userId, "수제 레몬에이드", "BEVERAGE", 500, "ML");
		fridgeItem(userId, "생수", "BEVERAGE", 2000, "ML");
		fridgeItem(userId, "딸기", "FRUIT", 300, "G");
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		ArgumentCaptor<MealPlanAiRequest> request = ArgumentCaptor.forClass(MealPlanAiRequest.class);
		verify(mealPlanAiAdapter).generate(request.capture());
		// "밀크 초콜릿"은 같은 카테고리(기타)의 사전 이름 "초콜릿"이 들어 있어 맞춰진다(#145)
		assertThat(request.getValue().ingredients()).extracting(FridgeStock::name)
				.containsExactlyInAnyOrder("대파", "생수", "딸기");
	}

	@Test
	void 보낼_냉장고_재료가_모두_빠지면_AI를_부르지_않고_재료_부족으로_실패한다() throws Exception {
		Long userId = createUser();
		fridgeItem(userId, "콜라", "BEVERAGE", 1500, "ML");

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, never()).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("MEAL_PLAN_NO_INGREDIENTS");
	}

	@Test
	void AI가_엉터리_조합을_만들면_한_번_더_불러_두_번째_응답으로_만든다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		// 첫 응답: 칸마다 대파 + 초콜릿(식약처에 파+초콜릿 짝이 없다)
		given(mealPlanAiAdapter.generate(any())).willAnswer(withChocolate(fridgeId))
				.willAnswer(everySlotUses(fridgeId, 100));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(2)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "COMPLETED");
		assertThat(count("SELECT COUNT(*) FROM recipe_ingredients ri JOIN recipes r ON r.id = ri.recipe_id "
				+ "WHERE r.user_id = ? AND ri.name = '초콜릿'", userId)).isZero();
	}

	@Test
	void AI가_두_번_다_엉터리_조합이면_AI_INVALID_RESPONSE로_실패하고_횟수는_그대로다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willAnswer(withChocolate(fridgeId));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(2)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("AI_INVALID_RESPONSE");
	}

	@Test
	void AI가_쓰지_않은_재료를_요리_이름에_넣으면_서버가_이름을_고쳐_저장한다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		// 이름은 "양파대파볶음"인데 양파는 재료에 없다
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> everySlotUses(fridgeId, 100).answer(invocation)
				.stream()
				.map(slot -> slot.withTitle("양파대파볶음"))
				.toList());

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		verify(mealPlanAiAdapter, times(1)).generate(any());
		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "COMPLETED");
		assertThat(jdbcTemplate.queryForList("SELECT DISTINCT title FROM recipes WHERE user_id = ?", String.class, userId))
				.containsExactly("대파볶음");
	}

	/** AI 가짜: 칸마다 대파 100g에 냉장고에 없는 초콜릿을 더 쓰는 레시피(엉터리 조합). */
	private static Answer<List<GeneratedMealSlot>> withChocolate(Long fridgeId) {
		return invocation -> everySlotUses(fridgeId, 100).answer(invocation).stream()
				.map(slot -> {
					List<SlotIngredient> ingredients = new ArrayList<>(slot.ingredients());
					ingredients.add(new SlotIngredient(null, "초콜릿", BigDecimal.valueOf(30), RecipeUnit.G));
					return new GeneratedMealSlot(slot.date(), slot.mealType(), "초코 대파 볶음", slot.cookMinutes(),
							slot.tips(), ingredients, slot.steps());
				})
				.toList();
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
	void 덮어쓰는_동안_오늘_칸을_요리하면_그_칸은_남기고_그_끼니는_새로_채우지_않는다() throws Exception {
		// 덮어쓸 칸은 AI를 부르기 전에 정한다. AI를 기다리는 사이 오늘 칸을 요리하면 완성 때 다시 확인해 남겨야 한다
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY, 5);
		Long today = slot(userId, oldPlan, TODAY, fridgeId, 100, false);
		Long tomorrow = slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);
		Long todayRecipe = jdbcTemplate.queryForObject("SELECT recipe_id FROM meal_plan_slots WHERE id = ?",
				Long.class, today);
		Answer<List<GeneratedMealSlot>> ai = everySlotUses(fridgeId, 100);
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			markMealSlotCookedUseCase.markCooked(userId, today, todayRecipe);
			return ai.answer(invocation);
		});

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long newPlan = newestPlanId(userId);
		assertThat(mealPlan(newPlan)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
		assertThat(slotDates(newPlan)).containsExactly(TODAY.plusDays(1), TODAY.plusDays(2), TODAY.plusDays(3),
				TODAY.plusDays(4));
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ? AND cooked_at IS NOT NULL", today))
				.isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", tomorrow)).isZero();
		// 남긴 끼니의 레시피도 만들지 않는다
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ? AND source = 'MEAL_PLAN'", userId))
				.isEqualTo(4);
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

		verify(mealPlanAiAdapter, times(2)).generate(any());
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

	// ---- 부족 재료 장바구니(#91) ----

	@Test
	void 부족_재료를_칸_출처로_담고_카테고리는_사전_냉장고_AI_순서로_정한다() throws Exception {
		Long userId = createUser();
		// 사전에 없는 이름의 냉장고 재료(채소). 100g이라 하루만 만든다
		Long fridgeId = fridgeGrams(userId, "우리집 대파", 100);
		aiUsesWith(fridgeId, 100, List.of(
				// 사전 이름(간장 = 양념)이 AI 값보다 먼저다. 같은 재료 두 줄은 한 행으로 합친다
				new SlotIngredient(null, "간장", BigDecimal.ONE, RecipeUnit.TBSP, IngredientCategoryCode.ETC),
				new SlotIngredient(null, "간장", BigDecimal.ONE, RecipeUnit.TBSP, IngredientCategoryCode.ETC),
				// 사전에 없으면 AI가 고른 값
				new SlotIngredient(null, "용과 소스", BigDecimal.valueOf(2), RecipeUnit.TBSP,
						IngredientCategoryCode.SEASONING),
				// 카테고리를 정하지 못하면 담지 않는다(식단표 저장은 계속)
				new SlotIngredient(null, "트러플 오일", BigDecimal.ONE, RecipeUnit.TSP),
				// 물과 양 없는 재료는 담지 않는다
				new SlotIngredient(null, "물", BigDecimal.valueOf(200), RecipeUnit.ML),
				new SlotIngredient(null, "소금", null, RecipeUnit.TO_TASTE)));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 1);
		Map<String, Object> slot = jdbcTemplate.queryForMap(
				"SELECT id, recipe_id FROM meal_plan_slots WHERE meal_plan_id = ?", planId);
		assertThat(jdbcTemplate.queryForList("""
				SELECT name, category_code FROM recipe_ingredients WHERE recipe_id = ? ORDER BY sort_order
				""", slot.get("recipe_id"))).extracting(row -> row.get("name"), row -> row.get("category_code"))
				.containsExactly(
						tuple("우리집 대파", "VEGETABLE"),
						tuple("간장", "SEASONING"),
						tuple("간장", "SEASONING"),
						tuple("용과 소스", "SEASONING"),
						tuple("트러플 오일", null),
						tuple("물", "BEVERAGE"),
						tuple("소금", "SEASONING"));
		assertThat(jdbcTemplate.queryForList("""
				SELECT c.name, c.amount, c.unit, g.code, c.recipe_id, c.meal_plan_slot_id
				FROM cart_items c JOIN ingredient_categories g ON g.id = c.category_id
				WHERE c.user_id = ? ORDER BY c.name
				""", userId)).extracting(row -> row.get("name"),
						row -> ((BigDecimal) row.get("amount")).stripTrailingZeros(), row -> row.get("unit"),
						row -> row.get("code"), row -> row.get("recipe_id"), row -> row.get("meal_plan_slot_id"))
				.containsExactly(
						tuple("간장", BigDecimal.valueOf(2), "TBSP", "SEASONING", slot.get("recipe_id"), slot.get("id")),
						tuple("용과 소스", BigDecimal.valueOf(2), "TBSP", "SEASONING", slot.get("recipe_id"),
								slot.get("id")));
		assertThat(eventProperty("meal_plan_generated", "cart_added_count")).isEqualTo("2");
		assertThat(eventProperty("cart_item_added", "cart_source")).isEqualTo("meal_plan");
		assertThat(eventProperty("cart_item_added", "item_count")).isEqualTo("2");
		assertThat(completedMessage(planId)).endsWith("부족한 재료 2개를 장바구니에 담았어요");
	}

	@Test
	void 덮어쓰면_지운_칸에서_담고_손대지_않은_장바구니_행은_지우고_고친_행은_연결만_비운다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY.plusDays(1), 1);
		Long oldSlot = slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);
		Long oldRecipe = jdbcTemplate.queryForObject("SELECT recipe_id FROM meal_plan_slots WHERE id = ?", Long.class,
				oldSlot);
		Long untouched = cartItem(userId, oldRecipe, oldSlot, "굴소스", 3, false);
		Long edited = cartItem(userId, oldRecipe, oldSlot, "고춧가루", 3, true);
		aiUses(fridgeId, 100);

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", oldSlot)).isZero();
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE id = ?", untouched)).isZero();
		assertThat(jdbcTemplate.queryForMap("SELECT recipe_id, meal_plan_slot_id, amount FROM cart_items WHERE id = ?",
				edited)).containsEntry("recipe_id", oldRecipe).containsEntry("meal_plan_slot_id", null)
				.satisfies(row -> assertThat(((BigDecimal) row.get("amount")).stripTrailingZeros())
						.isEqualTo(BigDecimal.valueOf(3)));
	}

	@Test
	void 장바구니_담기가_실패하면_식단표_칸_장바구니_횟수가_모두_그대로다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		Long oldPlan = completedPlan(userId, TODAY.plusDays(1), 1);
		Long oldSlot = slot(userId, oldPlan, TODAY.plusDays(1), fridgeId, 100, false);
		// 같은 재료 두 줄을 합치면 장바구니 수량 한도(정수 7자리)를 넘어 담기가 INVALID_INPUT으로 실패한다
		aiUsesWith(fridgeId, 100, List.of(
				new SlotIngredient(null, "간장", BigDecimal.valueOf(9_999_999), RecipeUnit.ML),
				new SlotIngredient(null, "간장", BigDecimal.valueOf(9_999_999), RecipeUnit.ML)));

		generate(userId, body(TODAY, "[\"DINNER\"]", true)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "FAILED");
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE id = ?", oldSlot)).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM meal_plan_slots WHERE meal_plan_id = ?", newestPlanId(userId)))
				.isZero();
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ? AND source = 'MEAL_PLAN' AND title LIKE '대파 볶음 %'",
				userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ?", userId)).isZero();
		assertThat(eventProperty("meal_plan_generate_failed", "error_code")).isEqualTo("INVALID_INPUT");
	}

	// ---- 이름이 같은 냉장고 재료(#91, 11-26 실측) ----

	@Test
	void AI가_냉장고_재료를_사야_할_재료로_쓰면_냉장고_재료로_다시_연결하고_장바구니에_담지_않는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "우리집 대파", 1000);
		aiUsesWith(fridgeId, 100, List.of(new SlotIngredient(null, " 우리집 대파 ", BigDecimal.valueOf(50), RecipeUnit.G)));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
		assertThat(count("""
				SELECT COUNT(*) FROM recipe_ingredients i JOIN recipes r ON r.id = i.recipe_id
				WHERE r.user_id = ? AND i.name = '우리집 대파' AND i.ingredient_id IS NULL
				""", userId)).isZero();
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", userId)).isZero();
	}

	@Test
	void AI가_냉장고_재료를_나눠_사야_할_재료로_써도_합쳐서_남은_양을_넘으면_그날부터_자른다() throws Exception {
		Long userId = createUser();
		// 하루에 냉장고 100g + "사야 할" 100g = 200g. 300g이라 이틀째에 모자란다
		Long fridgeId = fridgeGrams(userId, "우리집 대파", 300);
		aiUsesWith(fridgeId, 100, List.of(new SlotIngredient(null, "우리집 대파", BigDecimal.valueOf(100), RecipeUnit.G)));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		Long planId = newestPlanId(userId);
		assertThat(mealPlan(planId)).containsEntry("status", "COMPLETED").containsEntry("generated_days", 1);
		// 냉장고 재료가 모자란 것은 장바구니가 아니라 일수 줄이기로 처리한다(#78)
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", userId)).isZero();
		assertThat(completedMessage(planId)).endsWith("재료가 부족해 1일치만 만들었어요");
	}

	@Test
	void 단위가_달라_냉장고_재료로_연결하지_못해도_이름이_같은_냉장고_재료가_있으면_장바구니에_담지_않는다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "우리집 대파", 1000);
		// 냉장고는 그램, AI는 단으로 씀(g ↔ 단은 바꿀 수 없다)
		aiUsesWith(fridgeId, 100, List.of(new SlotIngredient(null, "우리집 대파", BigDecimal.ONE, RecipeUnit.BUNCH)));

		generate(userId, body(TODAY, "[\"DINNER\"]", false)).andExpect(status().isAccepted());
		awaitMealPlanJobs();

		assertThat(mealPlan(newestPlanId(userId))).containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
		assertThat(count("""
				SELECT COUNT(*) FROM recipe_ingredients i JOIN recipes r ON r.id = i.recipe_id
				WHERE r.user_id = ? AND i.unit = 'BUNCH' AND i.ingredient_id IS NULL
				""", userId)).isEqualTo(5);
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", userId)).isZero();
	}
}
