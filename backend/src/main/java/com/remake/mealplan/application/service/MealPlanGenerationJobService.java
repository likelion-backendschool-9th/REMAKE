package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetFridgeUseCase;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.in.RunMealPlanGenerationUseCase;
import com.remake.mealplan.application.port.out.GenerateMealPlanAiPort;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.application.port.out.MealPlanAiRequest;
import com.remake.mealplan.domain.AllocatedSlot;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanStatus;
import com.remake.mealplan.domain.MealPlanTrimmer;
import com.remake.mealplan.domain.MealSchedule;
import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.MealType;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.mealplan.domain.StockLedger;
import com.remake.mealplan.domain.TrimResult;
import com.remake.notification.domain.MealPlanFailReason;
import com.remake.recipe.application.port.in.GetRecipeIngredientsUseCase;
import com.remake.recipe.application.port.in.RecipeIngredientLine;
import com.remake.user.application.port.in.GetUserGenerationPreferencesUseCase;
import com.remake.user.domain.AllergenCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 식단표 생성 서버 작업. 트랜잭션 없이 재료를 읽고 AI를 부른 뒤, 결과 저장만 MealPlanResultWriter의 한 트랜잭션에서 한다
 * (AI를 기다리는 동안 DB 연결을 잡고 있지 않게).
 *
 * <p>어떤 실패든 FAILED + 실패 알림으로 끝내고 예외를 밖으로 던지지 않는다. 실패하면 기존 식단과 횟수는 그대로다.
 * <p>AI 응답은 GeneratedMealPlanValidator로 검사하고, AI 오류(연결·시간 초과·응답 이상)면 한 번 더 부른다
 * (domain-rules "AI retries once automatically", 레시피 생성과 같은 기준).
 */
@Slf4j
@Service
public class MealPlanGenerationJobService implements RunMealPlanGenerationUseCase {

	static final String NO_INGREDIENTS_CODE = "MEAL_PLAN_NO_INGREDIENTS";
	static final String UNEXPECTED_CODE = "INTERNAL_ERROR";
	/** 첫 호출 + 재시도 1회 */
	static final int MAX_AI_ATTEMPTS = 2;
	private static final Set<ErrorCode> RETRYABLE = Set.of(ErrorCode.AI_UPSTREAM_ERROR, ErrorCode.AI_TIMEOUT,
			ErrorCode.AI_INVALID_RESPONSE);

	private final LoadMealPlanPort loadMealPlanPort;
	private final LoadMealPlanSlotPort loadMealPlanSlotPort;
	private final GetFridgeUseCase getFridgeUseCase;
	private final GetRecipeIngredientsUseCase getRecipeIngredientsUseCase;
	private final GetUserGenerationPreferencesUseCase getUserGenerationPreferencesUseCase;
	private final GenerateMealPlanAiPort generateMealPlanAiPort;
	private final MealPlanResultWriter resultWriter;
	private final Clock clock;
	private final Duration retryDelay;

	public MealPlanGenerationJobService(LoadMealPlanPort loadMealPlanPort, LoadMealPlanSlotPort loadMealPlanSlotPort,
			GetFridgeUseCase getFridgeUseCase, GetRecipeIngredientsUseCase getRecipeIngredientsUseCase,
			GetUserGenerationPreferencesUseCase getUserGenerationPreferencesUseCase,
			GenerateMealPlanAiPort generateMealPlanAiPort, MealPlanResultWriter resultWriter, Clock clock,
			@Value("${remake.meal-plan.ai-retry-delay:1500ms}") Duration retryDelay) {
		this.loadMealPlanPort = loadMealPlanPort;
		this.loadMealPlanSlotPort = loadMealPlanSlotPort;
		this.getFridgeUseCase = getFridgeUseCase;
		this.getRecipeIngredientsUseCase = getRecipeIngredientsUseCase;
		this.getUserGenerationPreferencesUseCase = getUserGenerationPreferencesUseCase;
		this.generateMealPlanAiPort = generateMealPlanAiPort;
		this.resultWriter = resultWriter;
		this.clock = clock;
		this.retryDelay = retryDelay;
	}

	@Override
	public void run(MealPlanGenerationJob job) {
		try {
			generate(job);
		} catch (NoIngredientsException e) {
			fail(job, MealPlanFailReason.NO_INGREDIENTS, NO_INGREDIENTS_CODE);
		} catch (BusinessException e) {
			// AI 포트·검사기가 던지는 AI_UPSTREAM_ERROR·AI_TIMEOUT·AI_INVALID_RESPONSE(재시도까지 실패)
			log.warn("식단표 생성 실패 mealPlanId={} code={}", job.mealPlanId(), e.getErrorCode());
			fail(job, MealPlanFailReason.GENERIC, e.getErrorCode().name());
		} catch (RuntimeException e) {
			log.error("식단표 생성 중 예상하지 못한 오류 mealPlanId={}", job.mealPlanId(), e);
			fail(job, MealPlanFailReason.GENERIC, UNEXPECTED_CODE);
		}
	}

	@Override
	public void abandon(MealPlanGenerationJob job) {
		log.error("식단표 생성 작업을 시작하지 못함 mealPlanId={}", job.mealPlanId());
		fail(job, MealPlanFailReason.GENERIC, UNEXPECTED_CODE);
	}

	private void generate(MealPlanGenerationJob job) {
		MealPlan plan = loadMealPlanPort.loadById(job.mealPlanId())
				.orElseThrow(() -> new IllegalStateException("식단표가 없습니다: " + job.mealPlanId()));
		if (plan.status() != MealPlanStatus.GENERATING) {
			log.warn("이미 끝난 식단표라 작업을 건너뜀 mealPlanId={} status={}", plan.id(), plan.status());
			return;
		}
		LocalDate today = LocalDate.now(clock);
		LocalDate lastRequestedDay = plan.startDate().plusDays(job.maxDays() - 1L);

		// 내 다른 식단표의 오늘 이후 칸. 새 식단표에는 아직 칸이 없다.
		List<MealPlanSlot> futureSlots = loadMealPlanSlotPort.loadUserSlotsFrom(plan.userId(), today);
		List<MealPlanSlot> slotsInRange = futureSlots.stream()
				.filter(slot -> !slot.slotDate().isBefore(plan.startDate()) && !slot.slotDate().isAfter(lastRequestedDay))
				.toList();
		Set<MealSlotKey> keptSlots = slotsInRange.stream()
				.filter(MealPlanSlot::isCooked)
				.map(slot -> new MealSlotKey(slot.slotDate(), slot.mealType()))
				.collect(Collectors.toSet());
		// 덮어쓰기는 요청 기간 전체를 바꾼다(#78 11-24): 기간 안의 요리 안 한 칸은 성공하면 모두 지우고,
		// 그 재료는 처음부터 이번 생성이 쓸 수 있다. 재료가 모자라 못 만든 뒤쪽 날은 빈 날이 된다.
		// 덮어쓰기 없이 겹치면 접수에서 막히므로, 덮어쓰기가 아니면 이 목록은 비어 있다.
		List<Long> replacedSlotIds = slotsInRange.stream()
				.filter(slot -> !slot.isCooked())
				.map(MealPlanSlot::id)
				.toList();
		MealSchedule schedule = new MealSchedule(plan.startDate(), job.maxDays(), plan.mealTypes(), keptSlots);

		List<FridgeStock> fridge = loadFridge(plan.userId());
		Set<Long> replaced = Set.copyOf(replacedSlotIds);
		List<MealPlanSlot> remainingSlots = futureSlots.stream()
				.filter(slot -> !replaced.contains(slot.id()))
				.toList();
		StockLedger ledger = StockLedger.of(fridge, allocatedSlots(plan.userId(), remainingSlots), today);
		// 시작일 전에 소비기한이 끝나는 재료는 식단표 어느 날에도 쓸 수 없어 보내지 않는다(usableUntilDay가 1 미만)
		List<FridgeStock> available = ledger.available().stream()
				.filter(stock -> !stock.expirationDate().isBefore(plan.startDate()))
				.toList();
		if (available.isEmpty()) {
			throw new NoIngredientsException();
		}

		// 설정의 제외 재료(알레르기)는 항상 AI에 "쓰지 말 재료"로 보낸다(domain-rules 공통 규칙, #201과 같은 기준)
		List<AllergenCode> excludedAllergens = getUserGenerationPreferencesUseCase.getPreferences(plan.userId())
				.excludedAllergenCodes();
		MealPlanAiRequest aiRequest = new MealPlanAiRequest(plan.purpose(), plan.servings(), schedule, available,
				excludedAllergens);
		List<GeneratedMealSlot> generated = generateWithRetry(plan.id(), schedule, aiRequest).stream()
				.map(slot -> keepOwnFridgeIds(slot, fridge))
				.toList();
		TrimResult trim = MealPlanTrimmer.trim(schedule,
				generated.stream().map(GeneratedMealSlot::toPlannedSlot).toList(), ledger);
		if (trim.hasNoDays()) {
			throw new NoIngredientsException();
		}

		resultWriter.complete(job, slotsToSave(schedule, trim.generatedDays(), generated), trim.generatedDays(),
				trim.shortages().size(), replacedSlotIds);
	}

	/** AI를 부르고 검사한다. AI 오류면 쉬었다가 한 번 더 부르고, 두 번 다 실패하면 마지막 오류를 던진다. */
	private List<GeneratedMealSlot> generateWithRetry(Long mealPlanId, MealSchedule schedule,
			MealPlanAiRequest request) {
		BusinessException last = null;
		for (int attempt = 1; attempt <= MAX_AI_ATTEMPTS; attempt++) {
			try {
				return GeneratedMealPlanValidator.validate(schedule, generateMealPlanAiPort.generate(request));
			} catch (BusinessException e) {
				if (!RETRYABLE.contains(e.getErrorCode())) {
					throw e;
				}
				last = e;
				log.info("식단표 AI 실패({}회차) mealPlanId={} code={}", attempt, mealPlanId, e.getErrorCode());
				if (attempt < MAX_AI_ATTEMPTS) {
					pause();
				}
			}
		}
		throw last;
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

	/** 남긴 날들에서 새로 채울 칸(날짜·끼니 순). 자르기를 통과했으므로 모두 있다. */
	private static List<GeneratedMealSlot> slotsToSave(MealSchedule schedule, int generatedDays,
			List<GeneratedMealSlot> generated) {
		Map<MealSlotKey, GeneratedMealSlot> byKey = generated.stream()
				.collect(Collectors.toMap(GeneratedMealSlot::key, Function.identity(), (first, second) -> first));
		List<GeneratedMealSlot> result = new ArrayList<>();
		for (LocalDate date : schedule.dates().subList(0, generatedDays)) {
			for (MealType mealType : schedule.mealTypesOn(date)) {
				result.add(byKey.get(new MealSlotKey(date, mealType)));
			}
		}
		return result;
	}

	/** 냉장고 재료(ACTIVE). 양·소비기한이 없는 재료는 계산할 수 없어 뺀다. */
	private List<FridgeStock> loadFridge(Long userId) {
		return getFridgeUseCase.getFridgeIngredients(userId, null, null, null).stream()
				.map(FridgeIngredient::ingredient)
				.filter(ingredient -> ingredient.amount() != null && ingredient.expirationDate() != null)
				.map(MealPlanGenerationJobService::toFridgeStock)
				.toList();
	}

	private static FridgeStock toFridgeStock(Ingredient ingredient) {
		return new FridgeStock(ingredient.id(), ingredient.name(), ingredient.amount(), ingredient.unit(),
				ingredient.expirationDate());
	}

	/** 요리하지 않은 칸들이 쓰기로 한 재료(그 칸 레시피의 재료 줄). */
	private List<AllocatedSlot> allocatedSlots(Long userId, List<MealPlanSlot> futureSlots) {
		List<MealPlanSlot> uncooked = futureSlots.stream().filter(slot -> !slot.isCooked()).toList();
		Map<Long, List<RecipeIngredientLine>> ingredients = getRecipeIngredientsUseCase.getIngredients(userId,
				uncooked.stream().map(MealPlanSlot::recipeId).distinct().toList());
		return uncooked.stream()
				.map(slot -> new AllocatedSlot(slot.slotDate(),
						ingredients.getOrDefault(slot.recipeId(), List.of()).stream()
								.map(MealPlanGenerationJobService::toSlotIngredient)
								.toList()))
				.toList();
	}

	private static SlotIngredient toSlotIngredient(RecipeIngredientLine line) {
		return new SlotIngredient(line.ingredientId(), line.name(), line.amount(), line.unit());
	}

	/** 내 냉장고에 없는 재료 id는 지운다(냉장고에 없는 재료로 본다). 다른 회원의 재료 id가 레시피에 저장되지 않게 한다. */
	private static GeneratedMealSlot keepOwnFridgeIds(GeneratedMealSlot slot, List<FridgeStock> fridge) {
		Set<Long> ownIds = fridge.stream().map(FridgeStock::ingredientId).collect(Collectors.toSet());
		List<SlotIngredient> ingredients = slot.ingredients().stream()
				.map(ingredient -> ingredient.usesFridge() && !ownIds.contains(ingredient.fridgeIngredientId())
						? new SlotIngredient(null, ingredient.name(), ingredient.amount(), ingredient.unit())
						: ingredient)
				.toList();
		return new GeneratedMealSlot(slot.date(), slot.mealType(), slot.title(), slot.cookMinutes(), slot.tips(),
				ingredients, slot.steps());
	}

	private void fail(MealPlanGenerationJob job, MealPlanFailReason reason, String errorCode) {
		try {
			resultWriter.fail(job, reason, errorCode);
		} catch (RuntimeException e) {
			log.error("식단표 실패 처리 중 오류 mealPlanId={}", job.mealPlanId(), e);
		}
	}

	/** 쓸 수 있는 냉장고 재료가 없거나 하루도 만들 수 없음. */
	private static final class NoIngredientsException extends RuntimeException {
		NoIngredientsException() {
			super(null, null, false, false);
		}
	}
}
