package com.remake.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
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
import com.remake.support.MutableClock;

// API 42 식단 칸 레시피 교체. 기본 시각 2026-03-04 10:00(서울). 일반 회원 하루 레시피 횟수 2회.
class MealSlotReplaceApiTest extends IntegrationTestSupport {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long me;
	private Long planId;
	/** 냉장고 대파 500g(기한 넉넉함) */
	private Long greenOnion;

	@BeforeEach
	void setUp() {
		me = createUser();
		planId = plan(me, "DIET", 2);
		greenOnion = fridgeGrams(me, "대파", 500, TODAY.plusDays(30));
	}

	// ---- 요청·준비 ----

	private ResultActions replace(Long userId, Long slotId) throws Exception {
		return mockMvc.perform(post("/api/v1/user/meal-plan-slots/{slotId}/replace", slotId)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId));
	}

	private Long plan(Long userId, String purpose, int servings) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, purpose, meal_types, servings, generated_days, status,
				                        created_at, updated_at)
				VALUES (?, ?, ?, 'LUNCH,DINNER', ?, 5, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, userId, TODAY.minusDays(1), purpose, servings, now, now);
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

	/** 식단표 레시피 하나. 냉장고 재료 fridgeId를 grams만큼 쓴다(fridgeId가 null이면 재료 줄 없음). */
	private Long recipe(Long userId, String title, Long fridgeId, int grams) {
		LocalDateTime now = LocalDateTime.now(clock);
		Long recipeId = jdbcTemplate.queryForObject("""
				INSERT INTO recipes (user_id, source, title, servings, cook_minutes, created_at)
				VALUES (?, 'MEAL_PLAN', ?, 2, 10, ?)
				RETURNING id
				""", Long.class, userId, title, now);
		if (fridgeId != null) {
			jdbcTemplate.update("""
					INSERT INTO recipe_ingredients (recipe_id, ingredient_id, name, amount, unit, sort_order, created_at)
					VALUES (?, ?, '대파', ?, 'G', 0, ?)
					""", recipeId, fridgeId, grams, now);
		}
		return recipeId;
	}

	private Long slot(Long mealPlanId, Long recipeId, LocalDate date, String mealType, boolean cooked) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, cooked_at, created_at,
				                             updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, mealPlanId, date, mealType, recipeId, cooked ? now : null, now, now);
	}

	/** 내 식단표의 칸. 레시피는 대파 grams를 쓴다. */
	private Long slot(LocalDate date, String mealType, String title, int grams) {
		return slot(planId, recipe(me, title, greenOnion, grams), date, mealType, false);
	}

	/** AI 가짜: 요청한 칸 하나에 냉장고 재료 grams와 냉장고에 없는 간장·두부를 쓰는 레시피. */
	private void aiUses(Long fridgeId, int grams) {
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> List.of(newSlot(invocation.getArgument(0),
				fridgeId, grams)));
	}

	private static GeneratedMealSlot newSlot(MealPlanAiRequest request, Long fridgeId, int grams) {
		LocalDate date = request.schedule().startDate();
		MealType mealType = request.schedule().mealTypesOn(date).iterator().next();
		return new GeneratedMealSlot(date, mealType, "대파두부볶음", 15, null, List.of(
				new SlotIngredient(fridgeId, "대파", BigDecimal.valueOf(grams), RecipeUnit.G),
				new SlotIngredient(null, "두부", BigDecimal.ONE, RecipeUnit.EA),
				new SlotIngredient(null, "간장", BigDecimal.ONE, RecipeUnit.TBSP),
				new SlotIngredient(null, "참기름", null, RecipeUnit.TO_TASTE)),
				List.of("대파를 썬다", "두부를 굽는다", "간장으로 볶는다"));
	}

	private Map<String, Object> slotRow(Long slotId) {
		return jdbcTemplate.queryForMap("SELECT recipe_id, cooked_at FROM meal_plan_slots WHERE id = ?", slotId);
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private int recipeUsage() {
		return count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ? AND type = 'RECIPE'", me);
	}

	private void useRecipeQuota(int times) {
		for (int i = 0; i < times; i++) {
			jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, 'RECIPE', ?)", me,
					LocalDateTime.now(clock));
		}
	}

	private List<Map<String, Object>> events(String eventName) {
		awaitEventWrites();
		return jdbcTemplate.queryForList("""
				SELECT properties->>'meal_type' AS meal_type, properties->>'date_relation' AS date_relation,
				       properties->>'remaining_quota' AS remaining_quota, properties->>'error_code' AS error_code,
				       properties->>'duration_ms' IS NOT NULL AS has_duration
				FROM events WHERE event_name = ? AND user_id = ?
				""", eventName, me);
	}

	/** 장바구니 행 하나(식단표에서 담은 것). updated_at은 created_at과 같다(손대지 않은 행). */
	private Long cartItem(Long userId, Long recipeId, Long slotId, String name, int amount, String unit) {
		return cartItem(userId, recipeId, slotId, name, amount, unit, false);
	}

	/** edited면 담은 뒤 수량을 고친 행(updated_at이 created_at보다 뒤). */
	private Long cartItem(Long userId, Long recipeId, Long slotId, String name, int amount, String unit,
			boolean edited) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, recipe_id, meal_plan_slot_id, category_id, name, amount, unit,
				                        created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, userId, recipeId, slotId, categoryId("SEASONING"), name, amount, unit, now,
				edited ? now.plusMinutes(5) : now);
	}

	/** 장바구니 행. 지워졌으면 null. */
	private Map<String, Object> cartRow(Long cartItemId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
				SELECT recipe_id, meal_plan_slot_id, amount, created_at = updated_at AS untouched
				FROM cart_items WHERE id = ?
				""", cartItemId);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	/** 그 레시피로 담은 장바구니 행(이름·단위·양·칸, 이름순). */
	private List<Map<String, Object>> cartRowsOf(Long recipeId) {
		return jdbcTemplate.queryForList("""
				SELECT name, unit, amount, meal_plan_slot_id FROM cart_items WHERE recipe_id = ? ORDER BY name
				""", recipeId);
	}

	private MealPlanAiRequest capturedRequest() {
		ArgumentCaptor<MealPlanAiRequest> request = ArgumentCaptor.forClass(MealPlanAiRequest.class);
		verify(mealPlanAiAdapter).replace(request.capture());
		return request.getValue();
	}

	// ---- 성공 ----

	@Test
	void 교체하면_칸의_레시피가_새_식단표_레시피로_바뀌고_하루_횟수를_1회_쓴다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		aiUses(greenOnion, 120);

		replace(me, target).andExpect(status().isOk())
				.andExpect(jsonPath("$.slotId").value(target))
				.andExpect(jsonPath("$.title").value("대파두부볶음"))
				.andExpect(jsonPath("$.ingredientSummary").value("대파 · 두부 · 간장"))
				.andExpect(jsonPath("$.extraIngredientCount").value(1))
				// 냉장고에 없는 두부·간장 2개를 담았다(#91)
				.andExpect(jsonPath("$.cartAddedCount").value(2));

		Long newRecipe = (Long) slotRow(target).get("recipe_id");
		assertThat(newRecipe).isNotEqualTo(oldRecipe);
		assertThat(jdbcTemplate.queryForMap("SELECT source, servings, title FROM recipes WHERE id = ?", newRecipe))
				.containsEntry("source", "MEAL_PLAN").containsEntry("servings", 2).containsEntry("title", "대파두부볶음");
		assertThat(count("SELECT COUNT(*) FROM recipe_ingredients WHERE recipe_id = ? AND ingredient_id = ?",
				newRecipe, greenOnion)).isEqualTo(1);
		// 바꾸기 전 레시피는 남는다
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE id = ?", oldRecipe)).isEqualTo(1);
		assertThat(recipeUsage()).isEqualTo(1);
		assertThat(count("SELECT COUNT(*) FROM usage_logs WHERE user_id = ? AND type = 'MEAL_PLAN'", me)).isZero();
		assertThat(events("meal_slot_replaced")).singleElement().satisfies(event -> assertThat(event)
				.containsEntry("meal_type", "DINNER").containsEntry("date_relation", "FUTURE")
				.containsEntry("remaining_quota", "1").containsEntry("has_duration", true));
	}

	@Test
	void 응답의_레시피_번호는_칸에_저장된_레시피다() throws Exception {
		Long target = slot(TODAY, "LUNCH", "대파국", 100);
		aiUses(greenOnion, 100);

		String body = replace(me, target).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		Long recipeId = ((Number) JsonPath.read(body, "$.recipeId")).longValue();
		assertThat(slotRow(target).get("recipe_id")).isEqualTo(recipeId);
		assertThat(events("meal_slot_replaced")).singleElement()
				.satisfies(event -> assertThat(event).containsEntry("date_relation", "TODAY"));
	}

	@Test
	void 오늘_요리한_칸을_교체하면_요리_완료_표시가_지워진다() throws Exception {
		Long cooked = slot(planId, recipe(me, "대파국", greenOnion, 100), TODAY, "LUNCH", true);
		aiUses(greenOnion, 100);

		replace(me, cooked).andExpect(status().isOk());

		assertThat(slotRow(cooked).get("cooked_at")).isNull();
	}

	@Test
	void AI에는_식단표의_목적_인원과_그_칸만_보내고_피할_요리_이름을_넣는다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		slot(TODAY.plusDays(1), "LUNCH", "대파라면", 50);
		slot(TODAY.plusDays(2), "LUNCH", "대파전", 50);
		aiUses(greenOnion, 100);

		replace(me, target).andExpect(status().isOk());

		MealPlanAiRequest request = capturedRequest();
		assertThat(request.purpose().name()).isEqualTo("DIET");
		assertThat(request.servings()).isEqualTo(2);
		assertThat(request.schedule().dates()).containsExactly(TODAY.plusDays(1));
		assertThat(request.schedule().mealTypesOn(TODAY.plusDays(1))).containsExactly(MealType.DINNER);
		// 바꾸기 전 레시피와 같은 날 다른 끼니만(다른 날은 안 넣음)
		assertThat(request.avoidTitles()).containsExactly("대파볶음밥", "대파라면");
		// 바꾸기 전 레시피가 쓰던 냉장고 재료
		assertThat(request.avoidMainIngredientIds()).containsExactly(greenOnion);
	}

	@Test
	void 이_칸이_쓰던_재료는_돌려받고_다른_칸이_쓰기로_한_양은_뺀다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 300);
		slot(TODAY.plusDays(2), "LUNCH", "대파전", 150);
		// 오늘 요리한 칸은 이미 냉장고에서 뺐으므로 배정량으로 세지 않는다
		slot(planId, recipe(me, "대파국", greenOnion, 40), TODAY, "LUNCH", true);
		aiUses(greenOnion, 100);

		replace(me, target).andExpect(status().isOk());

		assertThat(capturedRequest().ingredients()).singleElement().satisfies(stock -> {
			assertThat(stock.ingredientId()).isEqualTo(greenOnion);
			assertThat(stock.amount()).isEqualByComparingTo("350");
		});
	}

	@Test
	void 칸_날짜_전에_소비기한이_끝나는_재료는_보내지_않는다() throws Exception {
		Long target = slot(TODAY.plusDays(3), "DINNER", "대파볶음밥", 100);
		fridgeGrams(me, "시금치", 200, TODAY.plusDays(2));
		aiUses(greenOnion, 100);

		replace(me, target).andExpect(status().isOk());

		assertThat(capturedRequest().ingredients()).extracting(FridgeStock::name).containsExactly("대파");
	}

	// ---- 거절 ----

	@Test
	void 지난_날짜의_칸은_바꿀_수_없다() throws Exception {
		Long yesterday = slot(TODAY.minusDays(1), "DINNER", "대파볶음밥", 100);

		replace(me, yesterday).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_DATE_NOT_ALLOWED"));

		verify(mealPlanAiAdapter, never()).replace(any());
		assertThat(recipeUsage()).isZero();
	}

	@Test
	void 없거나_다른_사람의_칸은_SLOT_NOT_FOUND다() throws Exception {
		Long other = createUser();
		Long othersSlot = slot(plan(other, "BALANCED", 1), createRecipe(other), TODAY, "LUNCH", false);

		replace(me, othersSlot).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
		replace(me, 999_999L).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));

		verify(mealPlanAiAdapter, never()).replace(any());
	}

	@Test
	void 하루_횟수를_다_쓰면_AI를_부르지_않고_429와_quota_exceeded를_남긴다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		useRecipeQuota(2);

		replace(me, target).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("GENERATION_LIMIT_EXCEEDED"));

		verify(mealPlanAiAdapter, never()).replace(any());
		assertThat(slotRow(target).get("recipe_id")).isEqualTo(oldRecipe);
		assertThat(recipeUsage()).isEqualTo(2);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForMap("""
				SELECT properties->>'limit_feature' AS feature, properties->>'used_count' AS used
				FROM events WHERE event_name = 'quota_exceeded' AND user_id = ?
				""", me)).containsEntry("feature", "RECIPE").containsEntry("used", "2");
	}

	@Test
	void 쓸_수_있는_냉장고_재료가_없으면_AI를_부르지_않고_422다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		// 생성 뒤 냉장고의 대파를 다 써서 재료 행이 지워짐(레시피의 재료 id는 SET NULL)
		jdbcTemplate.update("DELETE FROM ingredients WHERE id = ?", greenOnion);

		replace(me, target).andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_NO_INGREDIENTS"));

		verify(mealPlanAiAdapter, never()).replace(any());
		assertThat(recipeUsage()).isZero();
		assertThat(events("meal_slot_replace_failed")).singleElement()
				.satisfies(event -> assertThat(event).containsEntry("error_code", "MEAL_PLAN_NO_INGREDIENTS"));
	}

	@Test
	void 다른_칸이_재료를_모두_가져갔으면_422다() throws Exception {
		// 이 칸 레시피는 냉장고 재료를 안 쓰고, 다른 칸이 대파 500g을 모두 쓰기로 함
		Long target = slot(planId, recipe(me, "두부조림", null, 0), TODAY.plusDays(1), "DINNER", false);
		slot(TODAY.plusDays(2), "DINNER", "대파전", 500);

		replace(me, target).andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("MEAL_PLAN_NO_INGREDIENTS"));

		verify(mealPlanAiAdapter, never()).replace(any());
	}

	// ---- AI 실패 ----

	@Test
	void AI가_두_번_실패하면_기존_레시피와_횟수가_그대로다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		given(mealPlanAiAdapter.replace(any())).willThrow(new BusinessException(ErrorCode.AI_TIMEOUT));

		replace(me, target).andExpect(status().isGatewayTimeout()).andExpect(jsonPath("$.code").value("AI_TIMEOUT"));

		verify(mealPlanAiAdapter, times(2)).replace(any());
		assertThat(slotRow(target).get("recipe_id")).isEqualTo(oldRecipe);
		assertThat(recipeUsage()).isZero();
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ?", me)).isEqualTo(1);
		assertThat(events("meal_slot_replace_failed")).singleElement().satisfies(event -> assertThat(event)
				.containsEntry("error_code", "AI_TIMEOUT").containsEntry("has_duration", true));
	}

	@Test
	void AI가_한_번_실패해도_다시_불러_성공하면_교체된다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		given(mealPlanAiAdapter.replace(any())).willThrow(new BusinessException(ErrorCode.AI_UPSTREAM_ERROR))
				.willAnswer(invocation -> List.of(newSlot(invocation.getArgument(0), greenOnion, 100)));

		replace(me, target).andExpect(status().isOk());

		verify(mealPlanAiAdapter, times(2)).replace(any());
		assertThat(recipeUsage()).isEqualTo(1);
		assertThat(events("meal_slot_replace_failed")).isEmpty();
	}

	@Test
	void 재료가_있는데_못_만듦으로_오면_다시_부르고_또_그러면_형식_오류다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> {
			MealPlanAiRequest request = invocation.getArgument(0);
			return List.of(GeneratedMealSlot.skipped(request.schedule().startDate(), MealType.DINNER));
		});

		replace(me, target).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_INVALID_RESPONSE"));

		verify(mealPlanAiAdapter, times(2)).replace(any());
		assertThat(recipeUsage()).isZero();
	}

	@Test
	void 남은_양보다_많이_쓰거나_냉장고_재료를_안_쓰면_형식_오류다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		// 첫 번째: 대파 600g(남은 양 500g 초과), 두 번째: 냉장고 재료 없이(다른 회원의 재료 id는 냉장고에 없는 재료로 바뀌고,
		// 내 냉장고에 같은 이름이 없어 다시 연결되지도 않는다)
		Long othersFridge = fridgeGrams(createUser(), "양배추", 500, TODAY.plusDays(30));
		given(mealPlanAiAdapter.replace(any()))
				.willAnswer(invocation -> List.of(newSlot(invocation.getArgument(0), greenOnion, 600)))
				.willAnswer(invocation -> {
					MealPlanAiRequest request = invocation.getArgument(0);
					LocalDate date = request.schedule().startDate();
					return List.of(new GeneratedMealSlot(date, request.schedule().mealTypesOn(date).iterator().next(),
							"양배추볶음", 10, null,
							List.of(new SlotIngredient(othersFridge, "양배추", BigDecimal.valueOf(100), RecipeUnit.G)),
							List.of("양배추를 썬다", "볶는다", "간한다")));
				});

		replace(me, target).andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("AI_INVALID_RESPONSE"));

		verify(mealPlanAiAdapter, times(2)).replace(any());
		assertThat(recipeUsage()).isZero();
	}

	@Test
	void AI를_기다리는_동안_칸이_지워지면_저장하지_않고_횟수도_쓰지_않는다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> {
			jdbcTemplate.update("DELETE FROM meal_plan_slots WHERE id = ?", target);
			return List.of(newSlot(invocation.getArgument(0), greenOnion, 100));
		});

		replace(me, target).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));

		assertThat(recipeUsage()).isZero();
		assertThat(count("SELECT COUNT(*) FROM recipes WHERE user_id = ? AND title = '대파두부볶음'", me)).isZero();
		assertThat(events("meal_slot_replace_failed")).singleElement().satisfies(event -> assertThat(event)
				.containsEntry("error_code", "SLOT_NOT_FOUND").containsEntry("has_duration", true));
		assertThat(events("meal_slot_replaced")).isEmpty();
	}

	@Test
	void AI를_기다리는_동안_자정이_지나면_지난_칸이라_저장하지_않는다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> {
			clock.setNow(TODAY.plusDays(1).atStartOfDay(MutableClock.ZONE));
			return List.of(newSlot(invocation.getArgument(0), greenOnion, 100));
		});

		replace(me, target).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_DATE_NOT_ALLOWED"));

		assertThat(recipeUsage()).isZero();
		assertThat(events("meal_slot_replace_failed")).singleElement().satisfies(event -> assertThat(event)
				.containsEntry("error_code", "PAST_DATE_NOT_ALLOWED").containsEntry("has_duration", true));
	}

	// ---- 장바구니(#91) ----

	@Test
	void 교체하면_새_레시피의_부족_재료를_그_칸_출처로_장바구니에_담는다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		aiUses(greenOnion, 120);

		replace(me, target).andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(2));

		Long newRecipe = (Long) slotRow(target).get("recipe_id");
		// 냉장고 재료(대파)와 양 없는 재료(참기름 약간)는 담지 않는다
		assertThat(cartRowsOf(newRecipe)).extracting(row -> row.get("name"), row -> row.get("unit"),
						row -> ((BigDecimal) row.get("amount")).stripTrailingZeros(), row -> row.get("meal_plan_slot_id"))
				.containsExactly(
						tuple("간장", "TBSP", BigDecimal.ONE, target),
						tuple("두부", "EA", BigDecimal.ONE, target));
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForMap("""
				SELECT properties->>'cart_source' AS source, properties->>'item_count' AS item_count
				FROM events WHERE event_name = 'cart_item_added' AND user_id = ?
				""", me)).containsEntry("source", "meal_plan").containsEntry("item_count", "2");
	}

	@Test
	void 새_레시피에_담을_부족_재료가_없으면_담은_개수는_0이다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> {
			MealPlanAiRequest request = invocation.getArgument(0);
			LocalDate date = request.schedule().startDate();
			return List.of(new GeneratedMealSlot(date, request.schedule().mealTypesOn(date).iterator().next(),
					"대파구이", 10, null, List.of(
							new SlotIngredient(greenOnion, "대파", BigDecimal.valueOf(100), RecipeUnit.G),
							new SlotIngredient(null, "소금", null, RecipeUnit.TO_TASTE),
							new SlotIngredient(null, "물", BigDecimal.valueOf(50), RecipeUnit.ML)),
					List.of("대파를 썬다", "굽는다", "소금을 뿌린다")));
		});

		replace(me, target).andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(0));

		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", me)).isZero();
	}

	@Test
	void 교체하면_바꾸기_전_레시피로_담고_손대지_않은_행은_지우고_고친_행은_연결만_비운다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		Long other = slot(TODAY.plusDays(2), "DINNER", "대파국", 100);
		Long untouched = cartItem(me, oldRecipe, target, "굴소스", 3, "TBSP");
		Long edited = cartItem(me, oldRecipe, target, "고춧가루", 3, "TBSP", true);
		Long otherRow = cartItem(me, (Long) slotRow(other).get("recipe_id"), other, "굴소스", 2, "TBSP");
		aiUses(greenOnion, 120);

		replace(me, target).andExpect(status().isOk());

		assertThat(cartRow(untouched)).isNull();
		// 고친 행은 원본 레시피·수량을 그대로 두고 칸 연결만 비운다. 수정 시각도 바꾸지 않는다
		assertThat(cartRow(edited)).containsEntry("recipe_id", oldRecipe).containsEntry("meal_plan_slot_id", null)
				.containsEntry("untouched", false);
		assertThat(((BigDecimal) cartRow(edited).get("amount")).stripTrailingZeros()).isEqualTo(BigDecimal.valueOf(3));
		// 다른 칸에서 담은 행은 건드리지 않는다
		assertThat(cartRow(otherRow)).containsEntry("meal_plan_slot_id", other);
	}

	@Test
	void 바꾸기_전_레시피를_저장해_뒀으면_그_행은_남기고_연결만_비운다() throws Exception {
		Long target = slot(TODAY.plusDays(1), "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		Long untouched = cartItem(me, oldRecipe, target, "굴소스", 3, "TBSP");
		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, false, ?)",
				me, oldRecipe, LocalDateTime.now(clock));
		aiUses(greenOnion, 120);

		replace(me, target).andExpect(status().isOk());

		assertThat(cartRow(untouched)).containsEntry("recipe_id", oldRecipe).containsEntry("meal_plan_slot_id", null);
	}

	@Test
	void 교체_저장이_실패하면_장바구니도_그대로다() throws Exception {
		Long target = slot(TODAY, "DINNER", "대파볶음밥", 100);
		Long oldRecipe = (Long) slotRow(target).get("recipe_id");
		Long oldRow = cartItem(me, oldRecipe, target, "굴소스", 3, "TBSP");
		// AI를 기다리는 동안 다른 요청이 하루 횟수를 다 써서 마지막 차감에서 실패한다
		given(mealPlanAiAdapter.replace(any())).willAnswer(invocation -> {
			useRecipeQuota(2);
			return List.of(newSlot(invocation.getArgument(0), greenOnion, 100));
		});

		replace(me, target).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("GENERATION_LIMIT_EXCEEDED"));

		assertThat(slotRow(target).get("recipe_id")).isEqualTo(oldRecipe);
		assertThat(cartRow(oldRow)).containsEntry("meal_plan_slot_id", target);
		assertThat(count("SELECT COUNT(*) FROM cart_items WHERE user_id = ?", me)).isEqualTo(1);
	}
}
