package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.mealplan.application.port.in.ReplaceMealSlotUseCase;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.application.port.out.MealPlanSlotPort;
import com.remake.mealplan.application.port.out.ReplaceMealSlotAiPort;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanTrimmer;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.mealplan.domain.StockLedger;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;
import com.remake.quota.domain.RecipeQuota;
import com.remake.recipe.application.port.in.GetRecipeListItemsUseCase;
import com.remake.recipe.application.port.in.RecipeListItem;
import com.remake.recipe.application.port.in.SaveMealPlanRecipesUseCase;
import com.remake.recipe.domain.IngredientSummary;
import com.remake.user.application.port.in.GetUserGenerationPreferencesUseCase;
import com.remake.user.domain.AllergenCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 식단 칸 레시피 교체(API 42, #176). 식단표 생성과 같은 AI·지시문으로 그 칸 하나를 새로 만든다.
 * <ol>
 * <li>내 칸인지, 지난 날짜가 아닌지(SLOT_NOT_FOUND, PAST_DATE_NOT_ALLOWED)</li>
 * <li>레시피 하루 횟수가 남았는지(GENERATION_LIMIT_EXCEEDED, quota_exceeded 이벤트). 레시피 생성·다시 받기와 같은 횟수다</li>
 * <li>쓸 수 있는 냉장고 재료: 다른 칸(요리 안 한 칸)이 쓰기로 한 양을 빼고, 이 칸이 쓰던 양은 돌려받는다.
 * 바꾸기 전 레시피가 쓰던 냉장고 재료는 주재료로 쓰지 말라고 보낸다(같은 요리가 다시 나오지 않게, 11-25 실측).
 * 칸 날짜 전에 소비기한이 끝나는 재료는 뺀다. 하나도 없으면 AI를 부르지 않는다(MEAL_PLAN_NO_INGREDIENTS)</li>
 * <li>AI 호출과 검사. 실패하면 잠깐 쉬고 1회 다시 시도한다(25초 × 2 + 쉬는 시간 &lt; Nginx 60초, #50)</li>
 * <li>한 트랜잭션에서 새 레시피 저장 + 칸의 레시피 변경(요리 완료 표시 지움) + 하루 횟수 차감</li>
 * </ol>
 * AI 호출은 트랜잭션 밖에서 한다. 실패는 차감하지 않고 meal_slot_replace_failed 이벤트로 남긴다
 * (재료 없음, AI 실패, AI 뒤 저장 직전에 칸이 없어졌거나 자정이 지난 경우).
 * 바꾸기 전 레시피와 장바구니 항목은 그대로 둔다(부족 재료 담기는 #91, 장바구니 자동 삭제 없음).
 */
@Slf4j
@Service
class MealSlotReplaceService implements ReplaceMealSlotUseCase {

	private static final int MAX_AI_ATTEMPTS = 2;
	private static final Set<ErrorCode> RETRYABLE = Set.of(ErrorCode.AI_UPSTREAM_ERROR, ErrorCode.AI_TIMEOUT,
			ErrorCode.AI_INVALID_RESPONSE);

	private final MealPlanSlotPort mealPlanSlotPort;
	private final LoadMealPlanSlotPort loadMealPlanSlotPort;
	private final LoadMealPlanPort loadMealPlanPort;
	private final MealPlanStockReader stockReader;
	private final GetRecipeListItemsUseCase getRecipeListItemsUseCase;
	private final GetUserGenerationPreferencesUseCase getUserGenerationPreferencesUseCase;
	private final ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase;
	private final GetUsageQuotasUseCase getUsageQuotasUseCase;
	private final SaveMealPlanRecipesUseCase saveMealPlanRecipesUseCase;
	private final ReplaceMealSlotAiPort aiPort;
	private final EventTracker eventTracker;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;
	private final Duration retryDelay;

	MealSlotReplaceService(MealPlanSlotPort mealPlanSlotPort, LoadMealPlanSlotPort loadMealPlanSlotPort,
			LoadMealPlanPort loadMealPlanPort, MealPlanStockReader stockReader,
			GetRecipeListItemsUseCase getRecipeListItemsUseCase,
			GetUserGenerationPreferencesUseCase getUserGenerationPreferencesUseCase,
			ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase, GetUsageQuotasUseCase getUsageQuotasUseCase,
			SaveMealPlanRecipesUseCase saveMealPlanRecipesUseCase, ReplaceMealSlotAiPort aiPort,
			EventTracker eventTracker, TransactionTemplate transactionTemplate, Clock clock,
			@Value("${remake.meal-plan.ai-retry-delay:1500ms}") Duration retryDelay) {
		this.mealPlanSlotPort = mealPlanSlotPort;
		this.loadMealPlanSlotPort = loadMealPlanSlotPort;
		this.loadMealPlanPort = loadMealPlanPort;
		this.stockReader = stockReader;
		this.getRecipeListItemsUseCase = getRecipeListItemsUseCase;
		this.getUserGenerationPreferencesUseCase = getUserGenerationPreferencesUseCase;
		this.consumeRecipeQuotaUseCase = consumeRecipeQuotaUseCase;
		this.getUsageQuotasUseCase = getUsageQuotasUseCase;
		this.saveMealPlanRecipesUseCase = saveMealPlanRecipesUseCase;
		this.aiPort = aiPort;
		this.eventTracker = eventTracker;
		this.transactionTemplate = transactionTemplate;
		this.clock = clock;
		this.retryDelay = retryDelay;
	}

	@Override
	public ReplacedMealSlot replace(Long userId, Long slotId) {
		long start = System.nanoTime();
		MealPlanSlot slot = mealPlanSlotPort.findByIdAndUserId(slotId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
		LocalDate today = LocalDate.now(clock);
		if (slot.isPast(today)) {
			throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
		}
		MealPlan plan = loadMealPlanPort.loadById(slot.mealPlanId())
				.orElseThrow(() -> new IllegalStateException("칸의 식단표가 없습니다: " + slot.mealPlanId()));
		requireQuota(userId);

		// 이 칸은 바꿀 것이라 배정 목록에서 뺀다(그 칸 레시피가 쓰던 재료를 새 레시피가 쓸 수 있다)
		List<MealPlanSlot> futureSlots = loadMealPlanSlotPort.loadUserSlotsFrom(userId, today);
		List<MealPlanSlot> otherSlots = futureSlots.stream().filter(other -> !other.id().equals(slot.id())).toList();
		List<FridgeStock> fridge = stockReader.loadFridge(userId);
		StockLedger ledger = stockReader.ledger(userId, fridge, otherSlots, today);
		List<FridgeStock> available = ledger.available().stream()
				.filter(stock -> !stock.expirationDate().isBefore(slot.slotDate()))
				.toList();
		if (available.isEmpty()) {
			trackFailure(userId, ErrorCode.MEAL_PLAN_NO_INGREDIENTS, start);
			throw new BusinessException(ErrorCode.MEAL_PLAN_NO_INGREDIENTS);
		}

		MealSchedule schedule = new MealSchedule(slot.slotDate(), 1, Set.of(slot.mealType()), Set.of());
		// 설정의 제외 재료(알레르기)는 항상 "쓰지 말 재료"로 보낸다(생성과 같은 기준)
		List<AllergenCode> excludedAllergens = getUserGenerationPreferencesUseCase.getPreferences(userId)
				.excludedAllergenCodes();
		// 바꾸기 전 레시피가 쓰던 냉장고 재료는 주재료로 쓰지 않게 한다(AI에 보내는 재료에 있는 것만)
		Set<Long> availableIds = available.stream().map(FridgeStock::ingredientId).collect(Collectors.toSet());
		List<Long> avoidMainIngredientIds = stockReader.fridgeIngredientIds(userId, slot.recipeId()).stream()
				.filter(availableIds::contains)
				.toList();
		MealPlanAiRequest request = new MealPlanAiRequest(plan.purpose(), plan.servings(), schedule, available,
				excludedAllergens, avoidTitles(userId, slot, otherSlots), avoidMainIngredientIds);

		GeneratedMealSlot generated;
		try {
			generated = generateWithRetry(slotId, schedule, request, fridge, ledger);
		} catch (BusinessException e) {
			trackFailure(userId, e.getErrorCode(), start);
			throw e;
		}

		Saved saved;
		try {
			saved = transactionTemplate.execute(status -> save(userId, slotId, plan.servings(), generated));
		} catch (BusinessException e) {
			// 저장은 롤백됐다. 확인과 저장 사이에 다른 요청이 마지막 횟수를 썼으면 레시피 생성(E41)과 같이 quota_exceeded만 남긴다.
			// AI를 기다리는 동안 칸이 지워졌거나(SLOT_NOT_FOUND) 자정이 지났으면(PAST_DATE_NOT_ALLOWED) AI는 불렀지만 결과를
			// 못 남긴 것이라 실패 이벤트로 센다(M10 응답 시간, M16 호출 수에서 빠지지 않게).
			if (e.getErrorCode() == ErrorCode.GENERATION_LIMIT_EXCEEDED) {
				trackQuotaExceeded(userId);
			} else {
				trackFailure(userId, e.getErrorCode(), start);
			}
			throw e;
		}

		eventTracker.track(EventName.MEAL_SLOT_REPLACED, Screen.MEAL_PLAN_CALENDAR, Map.of(
				"meal_type", slot.mealType().name().toLowerCase(java.util.Locale.ROOT),
				"date_relation", slot.slotDate().equals(today) ? "today" : "future",
				"duration_ms", elapsedMillis(start),
				"remaining_quota", saved.quota().remaining()), EventTrackCommand.ofUser(userId));

		IngredientSummary summary = IngredientSummary.of(
				generated.ingredients().stream().map(SlotIngredient::name).toList());
		return new ReplacedMealSlot(slotId, saved.recipeId(), generated.title(), summary.summary(),
				summary.extraCount());
	}

	/**
	 * 피할 요리 이름: 바꾸기 전 레시피와 같은 날 다른 끼니의 레시피. 칸 하나만 보내면 AI가 식단표의 다른 칸을 모르기 때문이다.
	 * AI가 만든 레시피 제목이라 사용자가 쓴 글이 아니다.
	 */
	private List<String> avoidTitles(Long userId, MealPlanSlot slot, List<MealPlanSlot> otherSlots) {
		List<Long> recipeIds = new ArrayList<>();
		recipeIds.add(slot.recipeId());
		otherSlots.stream()
				.filter(other -> other.slotDate().equals(slot.slotDate()))
				.map(MealPlanSlot::recipeId)
				.forEach(recipeIds::add);
		Map<Long, RecipeListItem> items = getRecipeListItemsUseCase.getListItems(userId, recipeIds);
		Set<String> titles = new LinkedHashSet<>();
		recipeIds.stream().map(items::get).filter(Objects::nonNull).map(RecipeListItem::title).forEach(titles::add);
		return List.copyOf(titles);
	}

	private void requireQuota(Long userId) {
		try {
			consumeRecipeQuotaUseCase.requireRecipeQuota(userId);
		} catch (BusinessException e) {
			if (e.getErrorCode() == ErrorCode.GENERATION_LIMIT_EXCEEDED) {
				trackQuotaExceeded(userId);
			}
			throw e;
		}
	}

	/** AI를 부르고 검사한다. AI 오류(연결·시간 초과·응답 이상·과부하)면 retryDelay 뒤 한 번만 다시 부른다. */
	private GeneratedMealSlot generateWithRetry(Long slotId, MealSchedule schedule, MealPlanAiRequest request,
			List<FridgeStock> fridge, StockLedger ledger) {
		BusinessException last = null;
		for (int attempt = 1; attempt <= MAX_AI_ATTEMPTS; attempt++) {
			try {
				return checked(schedule, aiPort.replace(request), fridge, ledger);
			} catch (BusinessException e) {
				if (!RETRYABLE.contains(e.getErrorCode())) {
					throw e;
				}
				last = e;
				log.info("식단 칸 교체 AI 실패({}회차) slotId={} code={}", attempt, slotId, e.getErrorCode());
				if (attempt < MAX_AI_ATTEMPTS) {
					pause();
				}
			}
		}
		throw last;
	}

	/**
	 * 생성과 같은 검사(칸 모양·레시피 한도) 뒤, 칸이 장부 안에서 배정되는지 본다: 냉장고 재료를 1개 이상 쓰고 남은 양을 넘지 않아야 한다.
	 * 쓸 냉장고 재료가 있는데 "못 만듦"(skipped)으로 오거나 배정되지 않으면 형식 오류로 보고 다시 부른다(생성은 그날에서 자르지만
	 * 교체는 칸이 하나뿐이라 자를 수 없다).
	 */
	private static GeneratedMealSlot checked(MealSchedule schedule, List<GeneratedMealSlot> generated,
			List<FridgeStock> fridge, StockLedger ledger) {
		GeneratedMealSlot slot = MealPlanStockReader.keepOwnFridgeIds(
				GeneratedMealPlanValidator.validate(schedule, generated).getFirst(), fridge);
		if (slot.skipped()) {
			throw invalid("재료가 있는데 못 만듦으로 옴");
		}
		if (MealPlanTrimmer.trim(schedule, List.of(slot.toPlannedSlot()), ledger).hasNoDays()) {
			throw invalid("냉장고 재료를 쓰지 않거나 남은 양보다 많이 씀");
		}
		return slot;
	}

	/** 저장 직전에 칸을 다시 읽는다. AI를 기다리는 동안 칸이 지워졌거나(삭제·덮어쓰기) 자정이 지났으면 저장하지 않는다. */
	private Saved save(Long userId, Long slotId, int servings, GeneratedMealSlot generated) {
		MealPlanSlot current = mealPlanSlotPort.findByIdAndUserId(slotId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.SLOT_NOT_FOUND));
		if (current.isPast(LocalDate.now(clock))) {
			throw new BusinessException(ErrorCode.PAST_DATE_NOT_ALLOWED);
		}
		Long recipeId = saveMealPlanRecipesUseCase.saveMealPlanRecipes(userId,
				List.of(MealPlanResultWriter.toRecipeCommand(generated, servings))).getFirst();
		current.replaceRecipe(recipeId);
		mealPlanSlotPort.update(current);
		return new Saved(recipeId, consumeRecipeQuotaUseCase.consumeRecipeQuota(userId));
	}

	private void pause() {
		if (retryDelay.isZero()) {
			return;
		}
		try {
			Thread.sleep(retryDelay.toMillis());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BusinessException(ErrorCode.AI_UPSTREAM_ERROR);
		}
	}

	private void trackFailure(Long userId, ErrorCode code, long start) {
		eventTracker.track(EventName.MEAL_SLOT_REPLACE_FAILED, Screen.MEAL_PLAN_CALENDAR, Map.of(
				"error_code", code.name(),
				"duration_ms", elapsedMillis(start)), EventTrackCommand.ofUser(userId));
	}

	private void trackQuotaExceeded(Long userId) {
		RecipeQuota quota = getUsageQuotasUseCase.getRecipeQuota(userId);
		eventTracker.track(EventName.QUOTA_EXCEEDED, Screen.MEAL_PLAN_CALENDAR, Map.of(
				"limit_feature", "recipe",
				"limit_total", quota.limit() + quota.bonusRemaining(),
				"used_count", quota.used()), EventTrackCommand.ofUser(userId));
	}

	/** 형식 오류. 어느 검사에서 실패했는지만 남기고 레시피 내용은 남기지 않는다. */
	private static BusinessException invalid(String reason) {
		log.warn("식단 칸 교체 AI 응답 검사 실패: {}", reason);
		return new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
	}

	private static long elapsedMillis(long startNanos) {
		return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
	}

	private record Saved(Long recipeId, RecipeQuota quota) {
	}
}
