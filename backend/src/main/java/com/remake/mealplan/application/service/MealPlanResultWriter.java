package com.remake.mealplan.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.remake.cart.application.port.in.AddCartItemsUseCase;
import com.remake.cart.application.port.in.CartAdditionCommand;
import com.remake.cart.application.port.in.CleanUpMealPlanSlotCartItemsUseCase;
import com.remake.cart.domain.CartSource;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.mealplan.application.port.in.MealPlanGenerationJob;
import com.remake.mealplan.application.port.out.GeneratedMealSlot;
import com.remake.mealplan.application.port.out.LoadMealPlanPort;
import com.remake.mealplan.application.port.out.LoadMealPlanSlotPort;
import com.remake.mealplan.application.port.out.SaveMealPlanPort;
import com.remake.mealplan.application.port.out.SaveMealPlanSlotPort;
import com.remake.mealplan.domain.FridgeStock;
import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanStatus;
import com.remake.mealplan.domain.MealSlotKey;
import com.remake.mealplan.domain.SlotIngredient;
import com.remake.notification.application.port.in.CreateMealPlanNotificationUseCase;
import com.remake.notification.domain.MealPlanFailReason;
import com.remake.quota.application.port.in.ConsumeMealPlanQuotaUseCase;
import com.remake.recipe.application.port.in.MealPlanRecipeCommand;
import com.remake.recipe.application.port.in.SaveMealPlanRecipesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 생성 작업의 결과를 한 트랜잭션으로 저장한다. 완성 때 하나라도 실패하면 모두 롤백되어 기존 식단·횟수가 그대로 남는다.
 * 이벤트(E53·E54)는 EventTracker가 커밋 뒤에 보낸다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class MealPlanResultWriter {

	private final LoadMealPlanPort loadMealPlanPort;
	private final LoadMealPlanSlotPort loadMealPlanSlotPort;
	private final SaveMealPlanPort saveMealPlanPort;
	private final SaveMealPlanSlotPort saveMealPlanSlotPort;
	private final SaveMealPlanRecipesUseCase saveMealPlanRecipesUseCase;
	private final ConsumeMealPlanQuotaUseCase consumeMealPlanQuotaUseCase;
	private final CreateMealPlanNotificationUseCase createMealPlanNotificationUseCase;
	private final AddCartItemsUseCase addCartItemsUseCase;
	private final CleanUpMealPlanSlotCartItemsUseCase cleanUpMealPlanSlotCartItemsUseCase;
	private final EventTracker eventTracker;
	private final Clock clock;

	/**
	 * 완성: 덮어쓸 칸 다시 확인·삭제 → 레시피 저장 → 칸 저장 → 부족 재료 장바구니 담기 → COMPLETED → 월 차감 → 완성 알림.
	 *
	 * <p>덮어쓸 칸은 AI를 부르기 전에 정했으므로, AI를 기다리는 사이 사용자가 그 칸을 요리했을 수 있다.
	 * 그래서 칸을 잠그고 다시 읽어, 그 사이 요리한 칸은 지우지 않고 그 날짜·끼니에는 새 칸(과 레시피)을 넣지 않는다
	 * (domain-rules: 요리한 칸은 덮어써도 남긴다). 요리 완료(MealSlotCookService)도 칸을 잠그고 읽어 둘이 겹치지 않는다.
	 * 생성 일수(generatedDays)는 그대로 둔다. 그날은 요리한 칸으로 채워져 있다.
	 *
	 * <p>부족 재료는 새로 저장한 칸의 레시피마다 원본 recipeId와 칸 id를 넣어 AddCartItemsUseCase(MEAL_PLAN)로 담는다(#91).
	 * 이미 장바구니에 있는 행은 담기 쪽이 건드리지 않고, 실제로 새로 넣은 행 수가 완성 알림과 E53 cart_added_count가 된다.
	 * 덮어써서 지우는 칸에서 담은 장바구니 행은 칸을 지우기 전에 정리한다: 손대지 않은 행은 지우고, 수량을 고친 행과
	 * 저장해 둔 레시피의 행은 칸 연결만 비운다(CleanUpMealPlanSlotCartItemsUseCase).
	 *
	 * @param slots           새로 채울 칸(날짜·끼니 순)
	 * @param replacedSlotIds 지울 기존 칸(AI 호출 전 기준, 요청 기간 안의 요리 안 한 칸 전부. 못 만든 뒤쪽 날도 지워 빈 날이 된다)
	 * @param fridge          AI에 보낼 때 읽은 냉장고 재료. 이름이 같은 냉장고 재료가 있는 줄은 장바구니에 담지 않는다
	 */
	@Transactional
	public void complete(MealPlanGenerationJob job, List<GeneratedMealSlot> slots, int generatedDays,
			int shortageCount, Collection<Long> replacedSlotIds, List<FridgeStock> fridge) {
		MealPlan plan = loadGenerating(job.mealPlanId()).orElseThrow(
				() -> new IllegalStateException("생성 중인 식단표가 아닙니다: " + job.mealPlanId()));
		plan.complete(generatedDays);

		List<MealPlanSlot> replacedNow = loadMealPlanSlotPort.loadByIdsForUpdate(replacedSlotIds);
		Set<MealSlotKey> cookedMeanwhile = replacedNow.stream()
				.filter(MealPlanSlot::isCooked)
				.map(slot -> new MealSlotKey(slot.slotDate(), slot.mealType()))
				.collect(Collectors.toSet());
		List<Long> deletedSlotIds = replacedNow.stream()
				.filter(slot -> !slot.isCooked())
				.map(MealPlanSlot::id)
				.toList();
		List<GeneratedMealSlot> slotsToSave = slots.stream()
				.filter(slot -> !cookedMeanwhile.contains(slot.key()))
				.toList();
		if (!cookedMeanwhile.isEmpty()) {
			log.info("생성 중 요리한 칸을 남김 mealPlanId={} count={}", plan.id(), cookedMeanwhile.size());
		}

		// 지울 칸에서 담은 장바구니 행을 칸을 지우기 전에 정리한다(#91, 지운 뒤면 SET NULL로 어느 칸 행인지 모른다)
		cleanUpMealPlanSlotCartItemsUseCase.cleanUp(plan.userId(), replacedNow.stream()
				.filter(slot -> !slot.isCooked())
				.collect(Collectors.toMap(MealPlanSlot::id, MealPlanSlot::recipeId)));
		saveMealPlanSlotPort.deleteAllById(deletedSlotIds);
		List<Long> recipeIds = saveMealPlanRecipesUseCase.saveMealPlanRecipes(plan.userId(),
				slotsToSave.stream().map(slot -> toRecipeCommand(slot, plan.servings())).toList());
		List<MealPlanSlot> newSlots = new ArrayList<>();
		for (int i = 0; i < slotsToSave.size(); i++) {
			GeneratedMealSlot slot = slotsToSave.get(i);
			newSlots.add(MealPlanSlot.create(plan.id(), slot.date(), slot.mealType(), recipeIds.get(i)));
		}
		List<MealPlanSlot> savedSlots = saveMealPlanSlotPort.saveAll(newSlots);
		int cartAddedCount = addShortagesToCart(plan.userId(), slotsToSave, recipeIds, savedSlots, fridge);
		saveMealPlanPort.update(plan);
		int remaining = consumeMealPlanQuotaUseCase.consumeMealPlanQuota(plan.userId()).remaining();
		createMealPlanNotificationUseCase.notifyMealPlanCompleted(plan.userId(), plan.id(), plan.startDate(),
				generatedDays, job.maxDays(), cartAddedCount);

		eventTracker.track(EventName.MEAL_PLAN_GENERATED, null, Map.of(
				"meal_plan_id", plan.id(),
				"meal_purpose", plan.purpose().name().toLowerCase(Locale.ROOT),
				"meal_type_count", plan.mealTypes().size(),
				"generated_days", generatedDays,
				"shortage_count", shortageCount,
				"cart_added_count", cartAddedCount,
				// 기존 식단표의 칸을 지우고 바꿨는지(겹침 모달에서 덮어쓰기를 골랐어도 지운 칸이 없으면 false)
				"was_overwritten", !deletedSlotIds.isEmpty(),
				"duration_ms", durationMillis(job.acceptedAt()),
				"remaining_quota", remaining),
				EventTrackCommand.ofUser(plan.userId()));
	}

	/**
	 * 실패: FAILED + 실패 알림. 이미 끝난 식단표면 아무것도 하지 않는다.
	 * 항상 새 트랜잭션: 작업을 시작하지 못했을 때는 접수 트랜잭션의 커밋 직후 콜백에서 불리는데,
	 * 그 자리에서 기본 전파(REQUIRED)를 쓰면 이미 끝난 트랜잭션에 참여만 하고 커밋되지 않는다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void fail(MealPlanGenerationJob job, MealPlanFailReason reason, String errorCode) {
		failGenerating(job.mealPlanId(), job.acceptedAt(), reason, errorCode);
	}

	/**
	 * 끊긴 작업 정리: 아직 GENERATING이면 실패로 끝낸다. 실패 알림·E54는 일반 실패와 같다.
	 * 처리했으면 true, 그 사이 다른 쪽(작업·다른 컨테이너의 정리)이 끝냈으면 false.
	 *
	 * @param acceptedAt 접수 시각(meal_plans.created_at, Clock 시간대). E54 duration_ms를 여기서부터 센다
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean failAbandoned(Long mealPlanId, LocalDateTime acceptedAt, String errorCode) {
		return failGenerating(mealPlanId, acceptedAt.atZone(clock.getZone()).toInstant(),
				MealPlanFailReason.GENERIC, errorCode);
	}

	private boolean failGenerating(Long mealPlanId, Instant acceptedAt, MealPlanFailReason reason,
			String errorCode) {
		Optional<MealPlan> generating = loadGenerating(mealPlanId);
		if (generating.isEmpty()) {
			log.warn("생성 중인 식단표가 아니라 실패 처리를 건너뜀 mealPlanId={}", mealPlanId);
			return false;
		}
		MealPlan plan = generating.get();
		plan.fail();
		saveMealPlanPort.update(plan);
		createMealPlanNotificationUseCase.notifyMealPlanFailed(plan.userId(), reason);
		eventTracker.track(EventName.MEAL_PLAN_GENERATE_FAILED, null, Map.of(
				"error_code", errorCode,
				"duration_ms", durationMillis(acceptedAt)),
				EventTrackCommand.ofUser(plan.userId()));
		return true;
	}

	/**
	 * 잠그고 읽어서 GENERATING인지 확인한다. 완성·실패·끊긴 작업 정리가 모두 이걸 거치므로
	 * 같은 식단표를 두 쪽이 동시에 끝내지 않는다(먼저 잠근 쪽만 처리하고, 뒤에 온 쪽은 GENERATING이 아니라 건너뜀).
	 */
	private Optional<MealPlan> loadGenerating(Long mealPlanId) {
		return loadMealPlanPort.loadByIdForUpdate(mealPlanId)
				.filter(plan -> plan.status() == MealPlanStatus.GENERATING);
	}

	/** 새로 저장한 칸들의 부족 재료를 담고 실제로 새로 넣은 행 수를 돌려준다. 담을 것이 없으면 담기를 부르지 않는다. */
	private int addShortagesToCart(Long userId, List<GeneratedMealSlot> slots, List<Long> recipeIds,
			List<MealPlanSlot> savedSlots, List<FridgeStock> fridge) {
		List<CartAdditionCommand> items = new ArrayList<>();
		for (int i = 0; i < slots.size(); i++) {
			items.addAll(MealPlanCartItems.shortages(slots.get(i), recipeIds.get(i), savedSlots.get(i).id(), fridge));
		}
		return items.isEmpty() ? 0 : addCartItemsUseCase.add(userId, CartSource.MEAL_PLAN, items);
	}

	private long durationMillis(Instant acceptedAt) {
		return Math.max(Duration.between(acceptedAt, clock.instant()).toMillis(), 0);
	}

	static MealPlanRecipeCommand toRecipeCommand(GeneratedMealSlot slot, int servings) {
		return new MealPlanRecipeCommand(slot.title(), servings, slot.cookMinutes(), slot.tips(),
				slot.ingredients().stream().map(MealPlanResultWriter::toRecipeIngredient).toList(), slot.steps());
	}

	private static MealPlanRecipeCommand.Ingredient toRecipeIngredient(SlotIngredient ingredient) {
		return new MealPlanRecipeCommand.Ingredient(ingredient.fridgeIngredientId(), ingredient.name(),
				ingredient.amount(), ingredient.unit(), ingredient.categoryCode());
	}
}
