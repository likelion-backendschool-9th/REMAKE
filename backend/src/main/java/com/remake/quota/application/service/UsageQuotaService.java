package com.remake.quota.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.quota.application.port.in.ConsumeMealPlanQuotaUseCase;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.application.port.in.GetPremiumBenefitUsageSinceUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.quota.application.port.out.RecipeRewardPort;
import com.remake.quota.application.port.out.UsageLogPort;
import com.remake.quota.domain.MealPlanQuota;
import com.remake.quota.domain.PlanQuota;
import com.remake.quota.domain.RecipeQuota;
import com.remake.quota.domain.UsageType;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;

import lombok.extern.slf4j.Slf4j;

/**
 * 생성 횟수 계산과 차감. 남은 횟수는 저장하지 않고 usage_logs를 세어 계산한다(초기화 작업 없음).
 * "오늘"·"이번 달"은 주입한 Clock(한국 시간)으로 정한다.
 * 기본 횟수 소진 후 만료되지 않은 보상 횟수를 차감한다.
 * <p>
 * remake.quota.enforced=false면 한도를 넘어도 막지 않는다(기록은 그대로 남김). 개발자 본인 키로 반복 테스트하는 로컬 전용이고,
 * 기본값은 true라 운영·테스트 설정에는 넣지 않는다.
 */
@Slf4j
@Service
class UsageQuotaService implements GetUsageQuotasUseCase, ConsumeRecipeQuotaUseCase, ConsumeMealPlanQuotaUseCase,
		GetPremiumBenefitUsageSinceUseCase {

	private final UsageLogPort usageLogPort;
	private final GetUserUseCase getUserUseCase;
	private final RecipeRewardPort rewards;
	private final Clock clock;
	private final boolean enforced;

	UsageQuotaService(UsageLogPort usageLogPort, GetUserUseCase getUserUseCase, RecipeRewardPort rewards, Clock clock,
			@Value("${remake.quota.enforced:true}") boolean enforced) {
		this.usageLogPort = usageLogPort;
		this.getUserUseCase = getUserUseCase;
		this.rewards = rewards;
		this.clock = clock;
		this.enforced = enforced;
		if (!enforced) {
			log.warn("생성 횟수 한도를 검사하지 않습니다(remake.quota.enforced=false). 로컬 전용 설정입니다");
		}
	}

	@Override
	@Transactional(readOnly = true)
	public UsageQuotas getUsageQuotas(Long userId) {
		PlanQuota plan = planOf(userId);
		LocalDate today = LocalDate.now(clock);
		return new UsageQuotas(recipeQuota(userId, plan, today), mealPlanQuota(userId, plan, today));
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
		if (enforced && quota.exhausted()) {
			throw limitExceeded(quota);
		}
		return quota;
	}

	@Override
	@Transactional
	public RecipeQuota consumeRecipeQuota(Long userId) {
		usageLogPort.lockUser(userId);
		RecipeQuota quota = getRecipeQuota(userId);
		if (enforced && quota.exhausted()) {
			throw limitExceeded(quota);
		}
		Long grantId = null;
		if (quota.used() >= quota.limit() && quota.bonusRemaining() > 0) {
			grantId = rewards.consume(userId, LocalDateTime.now(clock)).orElse(null);
			if (enforced && grantId == null) {
				throw limitExceeded(quota);
			}
		}
		usageLogPort.save(userId, UsageType.RECIPE, grantId);
		return new RecipeQuota(quota.limit(), quota.used() + 1,
				quota.bonusRemaining() - (grantId == null ? 0 : 1), quota.resetsAt());
	}

	@Override
	@Transactional(readOnly = true)
	public MealPlanQuota requireMealPlanQuota(Long userId) {
		MealPlanQuota quota = mealPlanQuota(userId, planOf(userId), LocalDate.now(clock));
		if (enforced && quota.remaining() == 0) {
			throw new BusinessException(ErrorCode.MEAL_PLAN_LIMIT_EXCEEDED);
		}
		return quota;
	}

	@Override
	@Transactional
	public MealPlanQuota consumeMealPlanQuota(Long userId) {
		usageLogPort.lockUser(userId);
		MealPlanQuota quota = mealPlanQuota(userId, planOf(userId), LocalDate.now(clock));
		if (enforced && quota.remaining() == 0) {
			throw new BusinessException(ErrorCode.MEAL_PLAN_LIMIT_EXCEEDED);
		}
		usageLogPort.save(userId, UsageType.MEAL_PLAN, null);
		return new MealPlanQuota(quota.limit(), quota.used() + 1, quota.maxDays());
	}

	@Override
	@Transactional(readOnly = true)
	public BenefitUsage since(Long userId, LocalDateTime from) {
		int mealPlans = usageLogPort.countSince(userId, UsageType.MEAL_PLAN, from);
		int recipeAboveBasic = usageLogPort.countRecipeUsageAboveDailyLimit(userId, from,
				PlanQuota.BASIC.recipeDaily());
		return new BenefitUsage(mealPlans, recipeAboveBasic);
	}

	private MealPlanQuota mealPlanQuota(Long userId, PlanQuota plan, LocalDate today) {
		int used = usageLogPort.countSince(userId, UsageType.MEAL_PLAN, today.withDayOfMonth(1).atStartOfDay());
		return new MealPlanQuota(plan.mealPlanMonthly(), used, plan.mealPlanMaxDays());
	}

	private RecipeQuota recipeQuota(Long userId, PlanQuota plan, LocalDate today) {
		int used = usageLogPort.countSince(userId, UsageType.RECIPE, today.atStartOfDay());
		return new RecipeQuota(plan.recipeDaily(), used, rewards.remaining(userId, LocalDateTime.now(clock)),
				today.plusDays(1).atStartOfDay());
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
