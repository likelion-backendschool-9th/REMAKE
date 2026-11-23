package com.remake.quota.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.quota.application.port.out.UsageLogPort;
import com.remake.quota.domain.MealPlanQuota;
import com.remake.quota.domain.PlanQuota;
import com.remake.quota.domain.RecipeQuota;
import com.remake.quota.domain.UsageType;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * 생성 횟수 계산과 차감. 남은 횟수는 저장하지 않고 usage_logs를 세어 계산한다(초기화 작업 없음).
 * "오늘"·"이번 달"은 주입한 Clock(한국 시간)으로 정한다.
 * 보상 횟수(generation_quota_grants)는 그 테이블이 생기기 전까지 0으로 계산한다(공유·장바구니 영역이 만들면 연결).
 */
@Service
@RequiredArgsConstructor
class UsageQuotaService implements GetUsageQuotasUseCase, ConsumeRecipeQuotaUseCase {

	/** 보상 테이블이 생기기 전까지 쓸 수 있는 보상 횟수 */
	private static final int NO_BONUS = 0;

	private final UsageLogPort usageLogPort;
	private final GetUserUseCase getUserUseCase;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public UsageQuotas getUsageQuotas(Long userId) {
		PlanQuota plan = planOf(userId);
		LocalDate today = LocalDate.now(clock);
		int mealPlanUsed = usageLogPort.countSince(userId, UsageType.MEAL_PLAN, today.withDayOfMonth(1).atStartOfDay());
		return new UsageQuotas(recipeQuota(userId, plan, today),
				new MealPlanQuota(plan.mealPlanMonthly(), mealPlanUsed, plan.mealPlanMaxDays()));
	}

	@Override
	@Transactional(readOnly = true)
	public RecipeQuota getRecipeQuota(Long userId) {
		return recipeQuota(userId, planOf(userId), LocalDate.now(clock));
	}

	@Override
	@Transactional(readOnly = true)
	public RecipeQuota requireRecipeQuota(Long userId) {
		RecipeQuota quota = getRecipeQuota(userId);
		if (quota.exhausted()) {
			throw limitExceeded(quota);
		}
		return quota;
	}

	@Override
	@Transactional
	public RecipeQuota consumeRecipeQuota(Long userId) {
		usageLogPort.lockUser(userId);
		RecipeQuota quota = getRecipeQuota(userId);
		if (quota.exhausted()) {
			throw limitExceeded(quota);
		}
		usageLogPort.save(userId, UsageType.RECIPE, null);
		return new RecipeQuota(quota.limit(), quota.used() + 1, quota.bonusRemaining(), quota.resetsAt());
	}

	private RecipeQuota recipeQuota(Long userId, PlanQuota plan, LocalDate today) {
		int used = usageLogPort.countSince(userId, UsageType.RECIPE, today.atStartOfDay());
		return new RecipeQuota(plan.recipeDaily(), used, NO_BONUS, today.plusDays(1).atStartOfDay());
	}

	private PlanQuota planOf(Long userId) {
		User user = getUserUseCase.getUser(userId).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		return PlanQuota.of(user.plan());
	}

	private static BusinessException limitExceeded(RecipeQuota quota) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("remaining", 0);
		details.put("resetsAt", quota.resetsAt());
		return new BusinessException(ErrorCode.GENERATION_LIMIT_EXCEEDED, details);
	}
}
