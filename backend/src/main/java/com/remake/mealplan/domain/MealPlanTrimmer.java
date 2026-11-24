package com.remake.mealplan.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.remake.mealplan.domain.TrimResult.Shortage;

/**
 * AI가 만든 칸들을 냉장고 재료 양에 맞춰 자른다(domain-rules 5장 "만들 수 있는 일수까지만").
 *
 * <p>시작일부터 하루씩 본다. 그날 채울 칸이 모두 있고 모두 장부 안에서 배정되면 그날을 확정한다.
 * 칸 하나라도 없거나 배정할 수 없으면 그날부터 멈춘다(뒤 날이 되더라도 건너뛰지 않는다).
 * 장부는 AI에 보낸 재료 양과 같은 기준이다(덮어써서 지울 칸은 처음부터 빠져 있음, {@link StockLedger}).
 * 요청하지 않은 칸은 무시한다(형식 검사에서 거른다).
 */
public final class MealPlanTrimmer {

	private MealPlanTrimmer() {
	}

	public static TrimResult trim(MealSchedule schedule, List<PlannedSlot> generated, StockLedger ledger) {
		Map<MealSlotKey, PlannedSlot> byKey = generated.stream()
				.collect(Collectors.toMap(PlannedSlot::key, Function.identity(), (first, second) -> first));
		StockLedger current = ledger;
		int days = 0;
		List<Shortage> shortages = new ArrayList<>();
		for (LocalDate date : schedule.dates()) {
			Optional<List<PlannedSlot>> slots = slotsOn(date, schedule.mealTypesOn(date), byKey);
			if (slots.isEmpty()) {
				break;
			}
			Optional<StockLedger> next = current.reserveDay(slots.get());
			if (next.isEmpty()) {
				break;
			}
			current = next.get();
			days++;
			slots.get().forEach(slot -> addShortages(slot, shortages));
		}
		return new TrimResult(days, shortages);
	}

	/** 그날 채울 끼니의 칸들(끼니 순). 하나라도 없으면 비어 있다. */
	private static Optional<List<PlannedSlot>> slotsOn(LocalDate date, Set<MealType> mealTypes,
			Map<MealSlotKey, PlannedSlot> byKey) {
		List<PlannedSlot> slots = new ArrayList<>();
		for (MealType mealType : mealTypes) {
			PlannedSlot slot = byKey.get(new MealSlotKey(date, mealType));
			if (slot == null) {
				return Optional.empty();
			}
			slots.add(slot);
		}
		return Optional.of(slots);
	}

	private static void addShortages(PlannedSlot slot, List<Shortage> shortages) {
		for (SlotIngredient ingredient : slot.ingredients()) {
			if (!ingredient.usesFridge() && ingredient.isMeasured()) {
				shortages.add(new Shortage(slot.key(), ingredient));
			}
		}
	}
}
