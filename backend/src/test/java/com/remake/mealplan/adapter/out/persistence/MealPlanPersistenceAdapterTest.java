package com.remake.mealplan.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.application.port.out.SaveMealPlanPort;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealType;
import com.remake.support.IntegrationTestSupport;

// 식단표 저장 어댑터: 동시 접수를 막는 유니크 인덱스, 회원의 칸 찾기.
class MealPlanPersistenceAdapterTest extends IntegrationTestSupport {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private SaveMealPlanPort saveMealPlanPort;

	@Autowired
	private LoadMealPlanSlotPort loadMealPlanSlotPort;

	private MealPlan request(Long userId) {
		return MealPlan.request(userId, TODAY, MealPurpose.BALANCED, EnumSet.of(MealType.DINNER), 1, TODAY);
	}

	@Test
	void 같은_회원의_두_번째_GENERATING_저장은_MEAL_PLAN_IN_PROGRESS다() {
		Long userId = createUser();
		transactionTemplate.executeWithoutResult(status -> saveMealPlanPort.saveNew(request(userId)));

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
				status -> saveMealPlanPort.saveNew(request(userId))))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEAL_PLAN_IN_PROGRESS));
	}

	@Test
	void 회원의_칸만_날짜_끼니_순으로_찾는다() {
		Long userId = createUser();
		Long other = createUser();
		Long mine = plan(userId);
		Long others = plan(other);
		slot(mine, TODAY.plusDays(1), "BREAKFAST", userId);
		slot(mine, TODAY, "DINNER", userId);
		slot(mine, TODAY, "BREAKFAST", userId);
		slot(mine, TODAY.plusDays(5), "LUNCH", userId);
		slot(others, TODAY, "LUNCH", other);

		assertThat(loadMealPlanSlotPort.loadUserSlotsBetween(userId, TODAY, TODAY.plusDays(1)))
				.extracting(MealPlanSlot::slotDate, MealPlanSlot::mealType)
				.containsExactly(
						tuple(TODAY, MealType.BREAKFAST),
						tuple(TODAY, MealType.DINNER),
						tuple(TODAY.plusDays(1), MealType.BREAKFAST));
		assertThat(loadMealPlanSlotPort.loadUserSlotsFrom(userId, TODAY.plusDays(1))).hasSize(2);
	}

	private Long plan(Long userId) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO meal_plans (user_id, start_date, meal_types, servings, generated_days, status, created_at,
				                        updated_at)
				VALUES (?, ?, 'BREAKFAST,LUNCH,DINNER', 1, 7, 'COMPLETED', ?, ?)
				RETURNING id
				""", Long.class, userId, TODAY, now, now);
	}

	private void slot(Long mealPlanId, LocalDate date, String mealType, Long userId) {
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO meal_plan_slots (meal_plan_id, slot_date, meal_type, recipe_id, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?)
				""", mealPlanId, date, mealType, createRecipe(userId), now, now);
	}
}
