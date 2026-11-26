package com.remake.mealplan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

// API 41 식단 칸 삭제. 기본 시각 2026-03-04 10:00(서울).
class MealSlotDeleteApiTest extends IntegrationTestSupport {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long me;
	private Long planId;

	@BeforeEach
	void setUp() {
		me = createUser();
		planId = completedPlan(me, TODAY.minusDays(1), 5);
	}

	// ---- 요청·준비 ----

	private ResultActions deleteSlot(Long userId, Long slotId) throws Exception {
		return mockMvc.perform(delete("/api/v1/user/meal-plan-slots/{slotId}", slotId)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId));
	}

	private Long completedPlan(Long userId, LocalDate startDate, int days) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, purpose, meal_types, servings, generated_days, status,
				                        created_at, updated_at)
				VALUES (?, ?, 'BALANCED', 'LUNCH,DINNER', 1, ?, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, userId, startDate, days, now, now);
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

	private Long slot(LocalDate date, String mealType) {
		return slot(planId, createRecipe(me), date, mealType, false);
	}

	/** 식단표에서 담은 장바구니 행. edited면 담은 뒤 수량을 고친 행(updated_at이 created_at보다 뒤)이다. */
	private Long cartItemFrom(Long userId, Long recipeId, Long slotId, String name, boolean edited) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, recipe_id, meal_plan_slot_id, category_id, name, amount, unit,
				                        created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, 2, 'TBSP', ?, ?)
				RETURNING id
				""", Long.class, userId, recipeId, slotId, categoryId("SEASONING"), name, now,
				edited ? now.plusMinutes(5) : now);
	}

	private Map<String, Object> cartRow(Long cartItemId) {
		List<Map<String, Object>> rows = jdbcTemplate.queryForList(
				"SELECT meal_plan_slot_id, recipe_id, amount FROM cart_items WHERE id = ?", cartItemId);
		return rows.isEmpty() ? null : rows.getFirst();
	}

	private boolean slotExists(Long slotId) {
		return count("SELECT count(*) FROM meal_plan_slots WHERE id = ?", slotId) == 1;
	}

	private int count(String sql, Object... args) {
		return jdbcTemplate.queryForObject(sql, Integer.class, args);
	}

	private List<Map<String, Object>> deletedEvents() {
		awaitEventWrites();
		return jdbcTemplate.queryForList("""
				SELECT properties->>'meal_type' AS meal_type, properties->>'date_relation' AS date_relation
				FROM events WHERE event_name = 'meal_slot_deleted' AND user_id = ?
				""", me);
	}

	// ---- 삭제 ----

	@Test
	void 오늘_칸을_지우면_204이고_그_칸만_지워진다() throws Exception {
		Long target = slot(TODAY, "DINNER");
		Long sameDay = slot(TODAY, "LUNCH");
		Long tomorrow = slot(TODAY.plusDays(1), "DINNER");

		deleteSlot(me, target).andExpect(status().isNoContent());

		assertThat(slotExists(target)).isFalse();
		assertThat(slotExists(sameDay)).isTrue();
		assertThat(slotExists(tomorrow)).isTrue();
		assertThat(deletedEvents()).containsExactly(Map.of("meal_type", "dinner", "date_relation", "today"));
	}

	@Test
	void 앞으로의_칸도_지울_수_있다() throws Exception {
		Long future = slot(TODAY.plusDays(3), "LUNCH");

		deleteSlot(me, future).andExpect(status().isNoContent());

		assertThat(slotExists(future)).isFalse();
		assertThat(deletedEvents()).containsExactly(Map.of("meal_type", "lunch", "date_relation", "future"));
	}

	@Test
	void 오늘_요리한_칸도_지울_수_있다() throws Exception {
		Long cooked = slot(planId, createRecipe(me), TODAY, "LUNCH", true);

		deleteSlot(me, cooked).andExpect(status().isNoContent());

		assertThat(slotExists(cooked)).isFalse();
	}

	@Test
	void 칸을_지워도_레시피와_식단표_기간은_남는다() throws Exception {
		Long recipeId = createRecipe(me);
		Long target = slot(planId, recipeId, TODAY.plusDays(1), "DINNER", false);

		deleteSlot(me, target).andExpect(status().isNoContent());

		assertThat(count("SELECT count(*) FROM recipes WHERE id = ?", recipeId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForMap("SELECT status, generated_days FROM meal_plans WHERE id = ?", planId))
				.containsEntry("status", "COMPLETED").containsEntry("generated_days", 5);
	}

	@Test
	void 칸을_지우면_그_칸에서_담고_손대지_않은_장바구니_행은_지우고_고친_행은_연결만_비운다() throws Exception {
		Long recipeId = createRecipe(me);
		Long target = slot(planId, recipeId, TODAY.plusDays(1), "DINNER", false);
		Long other = slot(planId, createRecipe(me), TODAY.plusDays(2), "DINNER", false);
		Long untouched = cartItemFrom(me, recipeId, target, "간장", false);
		Long edited = cartItemFrom(me, recipeId, target, "굴소스", true);
		Long otherSlotRow = cartItemFrom(me, (Long) jdbcTemplate.queryForObject(
				"SELECT recipe_id FROM meal_plan_slots WHERE id = ?", Long.class, other), other, "간장", false);

		deleteSlot(me, target).andExpect(status().isNoContent());

		assertThat(cartRow(untouched)).isNull();
		assertThat(cartRow(edited)).containsEntry("meal_plan_slot_id", null).containsEntry("recipe_id", recipeId);
		assertThat(((Number) cartRow(edited).get("amount")).intValue()).isEqualTo(2);
		assertThat(cartRow(otherSlotRow)).containsEntry("meal_plan_slot_id", other);
	}

	@Test
	void 저장해_둔_레시피의_칸을_지우면_장바구니_행은_남고_연결만_비운다() throws Exception {
		Long recipeId = createRecipe(me);
		Long target = slot(planId, recipeId, TODAY.plusDays(1), "DINNER", false);
		Long untouched = cartItemFrom(me, recipeId, target, "간장", false);
		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, false, ?)",
				me, recipeId, LocalDateTime.now(clock));

		deleteSlot(me, target).andExpect(status().isNoContent());

		assertThat(cartRow(untouched)).containsEntry("meal_plan_slot_id", null).containsEntry("recipe_id", recipeId);
	}

	// ---- 거절 ----

	@Test
	void 지난_날짜_칸은_PAST_DATE_NOT_ALLOWED이고_지우지_않는다() throws Exception {
		Long yesterday = slot(TODAY.minusDays(1), "DINNER");

		deleteSlot(me, yesterday)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_DATE_NOT_ALLOWED"));

		assertThat(slotExists(yesterday)).isTrue();
		assertThat(deletedEvents()).isEmpty();
	}

	@Test
	void 자정이_지나면_어제가_된_칸은_지울_수_없다() throws Exception {
		Long todaySlot = slot(TODAY, "DINNER");
		clock.setNow(TODAY.plusDays(1).atStartOfDay(MutableClock.ZONE));

		deleteSlot(me, todaySlot)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAST_DATE_NOT_ALLOWED"));
	}

	@Test
	void 다른_사람의_칸은_SLOT_NOT_FOUND이고_지우지_않는다() throws Exception {
		Long other = createUser();
		Long otherPlan = completedPlan(other, TODAY, 3);
		Long othersSlot = slot(otherPlan, createRecipe(other), TODAY, "DINNER", false);

		deleteSlot(me, othersSlot)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));

		assertThat(slotExists(othersSlot)).isTrue();
	}

	@Test
	void 없는_칸이나_이미_지운_칸은_SLOT_NOT_FOUND다() throws Exception {
		Long target = slot(TODAY, "DINNER");
		deleteSlot(me, target).andExpect(status().isNoContent());

		deleteSlot(me, target)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
		deleteSlot(me, target + 1000)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SLOT_NOT_FOUND"));
		assertThat(deletedEvents()).hasSize(1);
	}
}
