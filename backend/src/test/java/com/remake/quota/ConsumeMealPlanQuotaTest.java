package com.remake.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.in.ConsumeMealPlanQuotaUseCase;
import com.remake.quota.domain.MealPlanQuota;
import com.remake.support.IntegrationTestSupport;

// 식단표 월 횟수 확인·차감. 기본 시각 2026-03-04 10:00(서울)이라 이번 달은 2026-03-01 00:00부터다.
class ConsumeMealPlanQuotaTest extends IntegrationTestSupport {

	private static final LocalDateTime MONTH_START = LocalDate.of(2026, 3, 1).atStartOfDay();

	@Autowired
	private ConsumeMealPlanQuotaUseCase consumeMealPlanQuotaUseCase;

	private void usage(Long userId, String type, LocalDateTime createdAt) {
		jdbcTemplate.update("INSERT INTO usage_logs (user_id, type, created_at) VALUES (?, ?, ?)", userId, type,
				createdAt);
	}

	private int mealPlanLogs(Long userId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM usage_logs WHERE user_id = ? AND type = 'MEAL_PLAN' AND grant_id IS NULL",
				Integer.class, userId);
	}

	@Test
	void 일반_회원은_월_1회_최대_5일이다() {
		Long userId = createUser();

		MealPlanQuota quota = consumeMealPlanQuotaUseCase.requireMealPlanQuota(userId);

		assertThat(quota).isEqualTo(new MealPlanQuota(1, 0, 5));
		assertThat(quota.remaining()).isEqualTo(1);
	}

	@Test
	void 구독_회원은_월_4회_최대_7일이다() {
		Long userId = createUser();
		jdbcTemplate.update("UPDATE users SET plan = 'PREMIUM' WHERE id = ?", userId);
		usage(userId, "MEAL_PLAN", MONTH_START);

		assertThat(consumeMealPlanQuotaUseCase.requireMealPlanQuota(userId)).isEqualTo(new MealPlanQuota(4, 1, 7));
	}

	@Test
	void 지난달_식단표와_레시피_기록은_세지_않는다() {
		Long userId = createUser();
		usage(userId, "MEAL_PLAN", MONTH_START.minusSeconds(1));
		usage(userId, "RECIPE", MONTH_START);

		assertThat(consumeMealPlanQuotaUseCase.requireMealPlanQuota(userId).used()).isZero();
	}

	@Test
	void 이번_달을_다_썼으면_확인에서_MEAL_PLAN_LIMIT_EXCEEDED다() {
		Long userId = createUser();
		usage(userId, "MEAL_PLAN", MONTH_START);

		assertThatThrownBy(() -> consumeMealPlanQuotaUseCase.requireMealPlanQuota(userId))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEAL_PLAN_LIMIT_EXCEEDED));
	}

	@Test
	void 차감하면_MEAL_PLAN_한_행을_남기고_차감_뒤_횟수를_돌려준다() {
		Long userId = createUser();

		MealPlanQuota after = transactionTemplate.execute(
				status -> consumeMealPlanQuotaUseCase.consumeMealPlanQuota(userId));

		assertThat(after).isEqualTo(new MealPlanQuota(1, 1, 5));
		assertThat(after.remaining()).isZero();
		assertThat(mealPlanLogs(userId)).isEqualTo(1);
	}

	@Test
	void 차감할_때_이미_다_썼으면_기록하지_않고_MEAL_PLAN_LIMIT_EXCEEDED다() {
		Long userId = createUser();
		usage(userId, "MEAL_PLAN", MONTH_START);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(
				status -> consumeMealPlanQuotaUseCase.consumeMealPlanQuota(userId)))
				.isInstanceOfSatisfying(BusinessException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MEAL_PLAN_LIMIT_EXCEEDED));
		assertThat(mealPlanLogs(userId)).isEqualTo(1);
	}
}
