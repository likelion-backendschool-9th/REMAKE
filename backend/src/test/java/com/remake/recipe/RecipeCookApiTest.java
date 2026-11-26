package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

// API 31 요리 후 차감 미리보기, API 32 요리 완료(#77, #63). 기본 시각 2026-03-04 10:00(서울)
class RecipeCookApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/recipes/";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private SaveRecipePort saveRecipePort;

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = createUser();
	}

	private Long recipe(Long owner, RecipeIngredient... ingredients) {
		return transactionTemplate.execute(status -> saveRecipePort.save(Recipe.create(owner, RecipeSource.QUICK,
				"요리", 2, 20, null, null, List.of(ingredients), List.of("만든다.")))).id();
	}

	private static RecipeIngredient line(String name, String amount, RecipeUnit unit) {
		return new RecipeIngredient(null, name, amount == null ? null : new BigDecimal(amount), unit);
	}

	private Long ingredient(Long owner, String name, String amount, String unit, LocalDate expiry) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, ?, ?, ?, ?, ?)
				RETURNING id
				""", Long.class, owner, categoryId("VEGETABLE"), name, expiry, new BigDecimal(amount),
				new BigDecimal(amount), unit, now, now);
	}

	private BigDecimal amountOf(Long ingredientId) {
		List<BigDecimal> rows = jdbcTemplate.queryForList("SELECT amount FROM ingredients WHERE id = ?",
				BigDecimal.class, ingredientId);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	private ResultActions preview(Long recipeId) throws Exception {
		return mockMvc.perform(get(URL + recipeId + "/cook-preview")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId));
	}

	private ResultActions cook(Long recipeId, String body) throws Exception {
		return mockMvc.perform(post(URL + recipeId + "/cook")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}

	private String cookCompletedProperties() {
		awaitEventWrites();
		return jdbcTemplate.queryForObject(
				"SELECT properties::text FROM events WHERE event_name = 'cook_completed' AND user_id = ?", String.class,
				userId);
	}

	private Long mealSlot(Long owner, Long recipeId, LocalDate date) {
		LocalDateTime now = LocalDateTime.now(clock);
		Long planId = jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, generated_days, status, created_at, updated_at)
				VALUES (?, ?, 'LUNCH', 1, 1, 'COMPLETED', ?, ?) RETURNING id
				""", Long.class, owner, date, now, now);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, created_at, updated_at)
				VALUES (?, ?, 'LUNCH', ?, ?, ?) RETURNING id
				""", Long.class, planId, date, recipeId, now, now);
	}

	private LocalDateTime cookedAt(Long slotId) {
		return jdbcTemplate.queryForObject("SELECT cooked_at FROM meal_plan_slots WHERE id = ?", LocalDateTime.class,
				slotId);
	}

	@Test
	void 미리보기는_같은_종류만_냉장고_단위로_바꾸고_못_바꾸면_양을_비운다() throws Exception {
		Long recipeId = recipe(userId,
				line("양파", "1", RecipeUnit.EA),
				line("돼지고기", "150", RecipeUnit.G),
				line("간장", "2", RecipeUnit.TBSP),
				line("우유", "300", RecipeUnit.ML),
				line("대파", "1", RecipeUnit.EA),
				line("물", "200", RecipeUnit.ML),
				line("소금", null, RecipeUnit.TO_TASTE),
				line("후추", "1", RecipeUnit.PINCH),
				line("버섯", "100", RecipeUnit.G));
		ingredient(userId, "양파", "3", "EA", TODAY.plusDays(10));
		Long soonOnion = ingredient(userId, "양파", "2", "EA", TODAY.plusDays(2));
		ingredient(userId, "양파", "5", "EA", TODAY.minusDays(1)); // 소비기한 지남
		Long pork = ingredient(userId, "돼지고기", "0.1", "KG", TODAY.plusDays(3));
		Long soy = ingredient(userId, "간장", "500", "ML", TODAY.plusDays(100));
		Long milk = ingredient(userId, "우유", "1", "L", TODAY);
		Long greenOnion = ingredient(userId, "대파", "2", "BUNCH", TODAY.plusDays(5));
		ingredient(createUser(), "버섯", "500", "G", TODAY.plusDays(5)); // 다른 사람 냉장고

		preview(recipeId).andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(5))
				// 같은 이름이면 소비기한이 가장 빠른 것(지난 것은 뺌)
				.andExpect(jsonPath("$.items[0].ingredientId").value(soonOnion))
				.andExpect(jsonPath("$.items[0].amount").value(1))
				.andExpect(jsonPath("$.items[0].unit").value("EA"))
				.andExpect(jsonPath("$.items[0].remainingAmount").value(2))
				.andExpect(jsonPath("$.items[0].imageUrl").isNotEmpty())
				// 150g → 0.15kg인데 남은 양 0.1kg까지만
				.andExpect(jsonPath("$.items[1].ingredientId").value(pork))
				.andExpect(jsonPath("$.items[1].amount").value(0.1))
				.andExpect(jsonPath("$.items[1].unit").value("KG"))
				// 2큰술 → 30ml
				.andExpect(jsonPath("$.items[2].ingredientId").value(soy))
				.andExpect(jsonPath("$.items[2].amount").value(30))
				// 300ml → 0.3L, 소비기한 오늘(D-DAY)은 쓸 수 있음
				.andExpect(jsonPath("$.items[3].ingredientId").value(milk))
				.andExpect(jsonPath("$.items[3].amount").value(0.3))
				.andExpect(jsonPath("$.items[3].unit").value("L"))
				// 1개 ↔ 단: 종류가 달라 사용자가 정함
				.andExpect(jsonPath("$.items[4].ingredientId").value(greenOnion))
				.andExpect(jsonPath("$.items[4].amount").doesNotExist())
				.andExpect(jsonPath("$.items[4].unit").value("BUNCH"));
	}

	@Test
	void 같은_냉장고_재료에_두_줄이_걸리면_양을_더한다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA), line("양파", "2", RecipeUnit.EA),
				line("버터", "1", RecipeUnit.TBSP), line("버터", "1", RecipeUnit.EA));
		ingredient(userId, "양파", "5", "EA", TODAY.plusDays(3));
		ingredient(userId, "버터", "200", "G", TODAY.plusDays(3));

		preview(recipeId).andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].amount").value(3))
				// 1큰술(부피)·1개 모두 g으로 못 바꿈
				.andExpect(jsonPath("$.items[1].amount").doesNotExist());
	}

	@Test
	void 요리_완료는_체크한_재료만_빼고_다_쓴_재료는_지우고_이벤트를_남긴다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA), line("간장", "2", RecipeUnit.TBSP),
				line("대파", "1", RecipeUnit.EA));
		Long onion = ingredient(userId, "양파", "1", "EA", TODAY.plusDays(3));
		Long soy = ingredient(userId, "간장", "500", "ML", TODAY.plusDays(30));
		Long greenOnion = ingredient(userId, "대파", "2", "BUNCH", TODAY.plusDays(3));

		cook(recipeId, """
				{"items":[{"ingredientId":%d,"amount":1},{"ingredientId":%d,"amount":45}]}
				""".formatted(onion, soy))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deductedCount").value(2));

		assertThat(amountOf(onion)).isNull();
		assertThat(amountOf(soy)).isEqualByComparingTo("455");
		assertThat(amountOf(greenOnion)).isEqualByComparingTo("2");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM ingredient_logs WHERE user_id = ? AND type = 'USED'", Integer.class, userId))
				.isEqualTo(2);
		// 양파는 미리보기 양(1) 그대로, 간장은 30 대신 45
		assertThat(cookCompletedProperties()).contains("\"recipe_id\": " + recipeId, "\"deducted_count\": 2",
				"\"adjusted_count\": 1", "\"from_meal_plan\": false");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM events WHERE event_name = 'ingredient_used' AND user_id = ?", Integer.class,
				userId)).isEqualTo(2);
	}

	@Test
	void 아무것도_체크하지_않아도_요리를_완료한다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA));

		cook(recipeId, "{\"items\":[]}").andExpect(status().isOk()).andExpect(jsonPath("$.deductedCount").value(0));
		assertThat(cookCompletedProperties()).contains("\"deducted_count\": 0", "\"adjusted_count\": 0");
	}

	@Test
	void 잘못된_재료가_하나라도_있으면_아무것도_빼지_않는다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA));
		Long onion = ingredient(userId, "양파", "3", "EA", TODAY.plusDays(3));
		Long garlic = ingredient(userId, "마늘", "2", "EA", TODAY.plusDays(3));
		Long others = ingredient(createUser(), "양파", "3", "EA", TODAY.plusDays(3));

		cook(recipeId, "{\"items\":[{\"ingredientId\":%d,\"amount\":0}]}".formatted(onion))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COOK_ITEM_INVALID"));
		cook(recipeId, "{\"items\":[{\"ingredientId\":%d,\"amount\":1},{\"ingredientId\":%d,\"amount\":1}]}"
				.formatted(onion, onion))
				.andExpect(jsonPath("$.code").value("COOK_ITEM_INVALID"));
		cook(recipeId, "{\"items\":[{\"ingredientId\":%d,\"amount\":1},{\"ingredientId\":%d,\"amount\":1}]}"
				.formatted(onion, others))
				.andExpect(jsonPath("$.code").value("COOK_ITEM_INVALID"));
		// 마늘이 모자라 실패하면 앞에서 뺀 양파도 되돌린다
		cook(recipeId, "{\"items\":[{\"ingredientId\":%d,\"amount\":1},{\"ingredientId\":%d,\"amount\":3}]}"
				.formatted(onion, garlic))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"));
		cook(recipeId, "{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));

		assertThat(amountOf(onion)).isEqualByComparingTo("3");
		assertThat(amountOf(garlic)).isEqualByComparingTo("2");
		assertThat(amountOf(others)).isEqualByComparingTo("3");
	}

	@Test
	void 볼_수_없는_레시피는_RECIPE_NOT_FOUND이고_저장한_남의_레시피는_된다() throws Exception {
		Long others = recipe(createUser(), line("양파", "1", RecipeUnit.EA));
		Long onion = ingredient(userId, "양파", "3", "EA", TODAY.plusDays(3));

		preview(others).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
		cook(others, "{\"items\":[]}").andExpect(status().isNotFound());

		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, false, ?)",
				userId, others, LocalDateTime.now(clock));
		// 남의 레시피라도 내 냉장고 재료와 이름으로 짝짓는다
		preview(others).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].ingredientId").value(onion));
	}

	@Test
	void 오늘_식단_칸에서_요리하면_그_칸만_요리_완료로_표시한다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA));
		Long todaySlot = mealSlot(userId, recipeId, TODAY);
		Long tomorrowSlot = mealSlot(userId, recipeId, TODAY.plusDays(1));

		cook(recipeId, "{\"items\":[],\"mealPlanSlotId\":%d}".formatted(todaySlot)).andExpect(status().isOk());
		assertThat(cookedAt(todaySlot)).isEqualTo(LocalDateTime.now(clock));
		assertThat(cookCompletedProperties()).contains("\"from_meal_plan\": true");

		cook(recipeId, "{\"items\":[],\"mealPlanSlotId\":%d}".formatted(tomorrowSlot)).andExpect(status().isOk());
		assertThat(cookedAt(tomorrowSlot)).isNull();
	}

	@Test
	void 칸이_없거나_다른_레시피의_칸이면_칸_표시만_건너뛰고_차감은_한다() throws Exception {
		Long recipeId = recipe(userId, line("양파", "1", RecipeUnit.EA));
		Long otherRecipe = recipe(userId, line("마늘", "1", RecipeUnit.EA));
		Long onion = ingredient(userId, "양파", "5", "EA", TODAY.plusDays(3));
		Long otherUser = createUser();
		Long othersSlot = mealSlot(otherUser, recipe(otherUser, line("양파", "1", RecipeUnit.EA)), TODAY);
		Long otherRecipeSlot = mealSlot(userId, otherRecipe, TODAY);

		Long deletedSlot = mealSlot(userId, recipeId, TODAY);
		jdbcTemplate.update("DELETE FROM meal_plan_slots WHERE id = ?", deletedSlot);

		// 다른 회원의 칸, 다른 레시피로 바뀐 칸(교체), 지워진 칸(삭제·덮어쓰기): 요리 완료는 성공하고 칸만 표시하지 않는다
		for (Long slot : List.of(othersSlot, otherRecipeSlot, deletedSlot)) {
			cook(recipeId, "{\"items\":[{\"ingredientId\":%d,\"amount\":1}],\"mealPlanSlotId\":%d}"
					.formatted(onion, slot))
					.andExpect(status().isOk()).andExpect(jsonPath("$.deductedCount").value(1));
		}
		// 세 번 모두 1개씩 뺐다
		assertThat(amountOf(onion)).isEqualByComparingTo("2");
		assertThat(cookedAt(othersSlot)).isNull();
		assertThat(cookedAt(otherRecipeSlot)).isNull();
	}
}
