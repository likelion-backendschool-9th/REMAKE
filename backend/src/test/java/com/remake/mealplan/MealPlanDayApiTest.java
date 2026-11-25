package com.remake.mealplan;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

// API 40 날짜별 끼니. 기본 시각 2026-03-04 10:00(서울).
class MealPlanDayApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/meal-plans/days/";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private ResultActions day(Long userId, String date) throws Exception {
		return mockMvc.perform(get(URL + date).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId));
	}

	private Long plan(Long userId, LocalDate startDate) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, generated_days, status, created_at,
				                        updated_at)
				VALUES (?, ?, 'BREAKFAST,LUNCH,DINNER', 1, 1, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, userId, startDate, now, now);
	}

	/** 식단표 레시피. 재료는 이름 순서대로 sort_order를 매긴다. */
	private Long recipe(Long userId, String title, String... ingredientNames) {
		LocalDateTime now = LocalDateTime.now(clock);
		Long recipeId = jdbcTemplate.queryForObject("""
				INSERT INTO recipes (user_id, source, title, servings, cook_minutes, created_at)
				VALUES (?, 'MEAL_PLAN', ?, 1, 10, ?)
				RETURNING id
				""", Long.class, userId, title, now);
		for (int i = 0; i < ingredientNames.length; i++) {
			jdbcTemplate.update("""
					INSERT INTO recipe_ingredients (recipe_id, name, amount, unit, sort_order, created_at)
					VALUES (?, ?, 1, 'EA', ?, ?)
					""", recipeId, ingredientNames[i], i, now);
		}
		return recipeId;
	}

	private Long slot(Long mealPlanId, LocalDate date, String mealType, Long recipeId, boolean cooked) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, cooked_at, created_at,
				                             updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, mealPlanId, date, mealType, recipeId, cooked ? now : null, now, now);
	}

	private void favorite(Long userId, Long recipeId) {
		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, true, ?)",
				userId, recipeId, LocalDateTime.now(clock));
	}

	@Test
	void 로그인_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL + TODAY)).andExpect(status().isUnauthorized());
	}

	@Test
	void 칸이_없는_날은_빈_목록이다() throws Exception {
		Long userId = createUser();

		day(userId, "2026-03-04")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.date").value("2026-03-04"))
				.andExpect(jsonPath("$.slots", empty()));
	}

	@Test
	void 그날_내_칸을_끼니_순으로_레시피_요약_즐겨찾기_요리_완료와_함께_돌려준다() throws Exception {
		Long userId = createUser();
		Long other = createUser();
		Long planId = plan(userId, TODAY);
		// 저장 순서를 끼니 순서와 다르게 넣는다(DB 문자열 정렬은 BREAKFAST, DINNER, LUNCH)
		Long dinnerRecipe = recipe(userId, "닭가슴살 샐러드", "닭가슴살", "청상추", "계란", "올리브유", "소금");
		Long dinner = slot(planId, TODAY, "DINNER", dinnerRecipe, false);
		Long breakfastRecipe = recipe(userId, "토스트", "식빵");
		Long breakfast = slot(planId, TODAY, "BREAKFAST", breakfastRecipe, true);
		Long lunchRecipe = recipe(userId, "비빔밥", "쌀", "고추장");
		Long lunch = slot(planId, TODAY, "LUNCH", lunchRecipe, false);
		favorite(userId, lunchRecipe);
		// 섞이면 안 되는 것: 다른 날 칸, 다른 회원 칸
		slot(planId, TODAY.plusDays(1), "DINNER", recipe(userId, "내일 저녁"), false);
		slot(plan(other, TODAY), TODAY, "LUNCH", recipe(other, "남의 점심"), false);

		day(userId, "2026-03-04")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[*].slotId")
						.value(contains(breakfast.intValue(), lunch.intValue(), dinner.intValue())))
				.andExpect(jsonPath("$.slots[*].mealType").value(contains("BREAKFAST", "LUNCH", "DINNER")))
				.andExpect(jsonPath("$.slots[0].recipeId").value(breakfastRecipe.intValue()))
				.andExpect(jsonPath("$.slots[0].title").value("토스트"))
				.andExpect(jsonPath("$.slots[0].ingredientSummary").value("식빵"))
				.andExpect(jsonPath("$.slots[0].extraIngredientCount").value(0))
				.andExpect(jsonPath("$.slots[0].favorite").value(false))
				.andExpect(jsonPath("$.slots[0].cooked").value(true))
				.andExpect(jsonPath("$.slots[1].favorite").value(true))
				.andExpect(jsonPath("$.slots[1].cooked").value(false))
				.andExpect(jsonPath("$.slots[2].ingredientSummary").value("닭가슴살 · 청상추 · 계란"))
				.andExpect(jsonPath("$.slots[2].extraIngredientCount").value(2));
	}

	@Test
	void 지난_날짜도_조회한다() throws Exception {
		Long userId = createUser();
		LocalDate past = TODAY.minusDays(10);
		Long slotId = slot(plan(userId, past), past, "DINNER", recipe(userId, "지난 저녁", "두부"), true);

		day(userId, past.toString())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[0].slotId").value(slotId.intValue()))
				.andExpect(jsonPath("$.slots[0].cooked").value(true));
	}

	@Test
	void 날짜_형식이_틀리면_INVALID_INPUT이다() throws Exception {
		Long userId = createUser();

		for (String date : List.of("2026-02-30", "20260304", "2026-3-4")) {
			day(userId, date)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
	}

	// ---- 화면 흐름: 생성 → 하루 조회 → 레시피 상세(API 30) → 요리 완료(API 32, mealPlanSlotId) ----

	@Test
	void 생성한_식단표를_하루_조회로_열고_그_칸에서_요리하면_요리_완료로_보인다() throws Exception {
		Long userId = createUser();
		Long fridgeId = fridgeGrams(userId, "대파", 1000);
		given(mealPlanAiAdapter.generate(any())).willAnswer(invocation -> {
			MealPlanAiRequest request = invocation.getArgument(0);
			List<GeneratedMealSlot> slots = new ArrayList<>();
			for (LocalDate date : request.schedule().dates()) {
				for (MealType mealType : request.schedule().mealTypesOn(date)) {
					slots.add(new GeneratedMealSlot(date, mealType, "대파 볶음 " + date, 15, null, List.of(
							new SlotIngredient(fridgeId, "대파", BigDecimal.valueOf(100), RecipeUnit.G),
							new SlotIngredient(null, "간장", BigDecimal.ONE, RecipeUnit.TBSP)),
							List.of("대파를 썬다", "볶는다")));
				}
			}
			return slots;
		});

		mockMvc.perform(post("/api/v1/user/meal-plans/generate")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"startDate":"%s","purpose":"BALANCED","mealTypes":["DINNER"],"servings":1}
								""".formatted(TODAY)))
				.andExpect(status().isAccepted());
		awaitMealPlanJobs();

		day(userId, TODAY.toString())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[0].mealType").value("DINNER"))
				.andExpect(jsonPath("$.slots[0].title").value("대파 볶음 " + TODAY))
				.andExpect(jsonPath("$.slots[0].ingredientSummary").value("대파 · 간장"))
				.andExpect(jsonPath("$.slots[0].cooked").value(false));
		Long slotId = jdbcTemplate.queryForObject("""
				SELECT s.id FROM meal_plan_slots s JOIN meal_plans p ON p.id = s.meal_plan_id
				WHERE p.user_id = ? AND s.slot_date = ?
				""", Long.class, userId, TODAY);
		Long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM meal_plan_slots WHERE id = ?", Long.class,
				slotId);

		mockMvc.perform(get("/api/v1/user/recipes/" + recipeId)
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("대파 볶음 " + TODAY));
		mockMvc.perform(post("/api/v1/user/recipes/" + recipeId + "/cook")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[],\"mealPlanSlotId\":%d}".formatted(slotId)))
				.andExpect(status().isOk());

		day(userId, TODAY.toString())
				.andExpect(jsonPath("$.slots[0].slotId").value(slotId.intValue()))
				.andExpect(jsonPath("$.slots[0].cooked").value(true));
	}

	private Long fridgeGrams(Long userId, String name, int grams) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, ?, ?, 'G', NULL, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId("VEGETABLE"), name, TODAY.plusDays(30), grams, grams, now, now);
	}
}
