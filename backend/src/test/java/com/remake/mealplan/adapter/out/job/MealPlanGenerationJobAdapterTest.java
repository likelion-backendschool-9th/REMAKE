package com.remake.mealplan.adapter.out.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskRejectedException;

import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.in.RunMealPlanGenerationUseCase;
import com.remake.support.IntegrationTestSupport;

// 작업 실행기가 작업을 받지 못하면(대기열이 가득 참) 식단표를 GENERATING에 두지 않고 바로 실패로 끝낸다.
class MealPlanGenerationJobAdapterTest extends IntegrationTestSupport {

	@Autowired
	private RunMealPlanGenerationUseCase runMealPlanGenerationUseCase;

	@Test
	void 접수가_커밋된_뒤_실행기가_거부하면_실패와_실패_알림을_남긴다() {
		Long userId = createUser();
		MealPlanGenerationJobAdapter adapter = new MealPlanGenerationJobAdapter(task -> {
			throw new TaskRejectedException("대기열이 가득 참");
		}, runMealPlanGenerationUseCase);

		// 운영처럼 접수 트랜잭션 안에서 저장하고 작업을 예약한다. 거부는 커밋 직후 콜백에서 일어난다.
		Long mealPlanId = transactionTemplate.execute(status -> {
			LocalDateTime now = LocalDateTime.now(clock);
			Long id = jdbcTemplate.queryForObject("""
					INSERT INTO meal_plans (user_id, start_date, meal_types, servings, status, created_at, updated_at)
					VALUES (?, ?, 'DINNER', 1, 'GENERATING', ?, ?)
					RETURNING id
					""", Long.class, userId, LocalDate.now(clock), now, now);
			adapter.request(new MealPlanGenerationJob(id, userId, 5, false, Instant.now(clock)));
			return id;
		});

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM meal_plans WHERE id = ?", String.class, mealPlanId))
				.isEqualTo("FAILED");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM notifications WHERE user_id = ? AND type = 'MEAL_PLAN_FAILED'", Integer.class,
				userId)).isEqualTo(1);
	}
}
