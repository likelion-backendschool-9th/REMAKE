package com.remake.mealplan.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.remake.mealplan.domain.StockUnits.BaseAmount;

/**
 * 냉장고 재료별 남은 양 장부. 같은 재료 양을 여러 칸에 배정하지 않게 한다(domain-rules 5장).
 *
 * <p>재료마다 냉장고 양에서 칸들이 쓰기로 한 양을 뺀다.
 * <ul>
 * <li>칸 재료가 냉장고 재료와 같은 종류의 단위면 기준 단위로 바꿔 뺀다.</li>
 * <li>종류가 달라 계산할 수 없으면(냉장고 2팩, 레시피 300g) 그 칸이 재료를 통째로 쓴 것으로 본다.</li>
 * <li>양이 없거나 꼬집·약간인 재료는 빼지 않는다.</li>
 * </ul>
 * 소비기한이 지난 재료는 처음부터 넣지 않는다(오늘까지인 재료는 쓴다).
 * 장부는 바꾸지 않고, 칸을 더하거나 뺄 때마다 새 장부를 돌려준다.
 */
public final class StockLedger {

	private final Map<Long, Stock> stocks;
	private final List<AllocatedSlot> allocated;

	private StockLedger(Map<Long, Stock> stocks, List<AllocatedSlot> allocated) {
		this.stocks = stocks;
		this.allocated = allocated;
	}

	/**
	 * 냉장고 재료에서 이미 있는 칸들의 배정량을 뺀 장부를 만든다.
	 *
	 * @param allocated 다른 식단표의 오늘 이후·요리 안 한 칸들
	 * @param today     소비기한이 지났는지 볼 오늘 날짜
	 */
	public static StockLedger of(List<FridgeStock> fridgeStocks, List<AllocatedSlot> allocated, LocalDate today) {
		if (today == null) {
			throw new IllegalArgumentException("오늘 날짜가 필요합니다.");
		}
		Map<Long, Stock> stocks = new LinkedHashMap<>();
		for (FridgeStock fridgeStock : fridgeStocks) {
			if (!fridgeStock.expirationDate().isBefore(today)) {
				stocks.put(fridgeStock.ingredientId(), Stock.of(fridgeStock));
			}
		}
		List<AllocatedSlot> slots = List.copyOf(allocated);
		StockLedger ledger = new StockLedger(stocks, slots);
		for (AllocatedSlot slot : slots) {
			ledger.claim(slot.ingredients());
		}
		return ledger;
	}

	/**
	 * 아직 쓸 수 있는 냉장고 재료와 남은 양(냉장고 단위). 남은 양이 0이면 뺀다.
	 * AI에 보낼 재료 목록이고, 비어 있으면 하루도 만들 수 없다(MEAL_PLAN_NO_INGREDIENTS).
	 */
	public List<FridgeStock> available() {
		List<FridgeStock> result = new ArrayList<>();
		for (Stock stock : stocks.values()) {
			BigDecimal remaining = stock.remaining();
			if (remaining.signum() > 0) {
				FridgeStock source = stock.source();
				result.add(new FridgeStock(source.ingredientId(), source.name(),
						StockUnits.fromBase(remaining, source.unit()), source.unit(), source.expirationDate()));
			}
		}
		return result;
	}

	/**
	 * 그날 칸들을 배정한 새 장부. 먼저 그날의 이미 있는 칸(덮어써서 지울 칸)의 배정량을 돌려받는다.
	 * 칸 하나라도 배정할 수 없으면 비어 있다(장부는 그대로).
	 */
	Optional<StockLedger> reserveDay(LocalDate date, List<PlannedSlot> slots) {
		StockLedger next = copy();
		for (AllocatedSlot slot : allocated) {
			if (slot.date().equals(date)) {
				next.unclaim(slot.ingredients());
			}
		}
		for (PlannedSlot slot : slots) {
			if (!next.tryClaim(slot.ingredients())) {
				return Optional.empty();
			}
		}
		return Optional.of(next);
	}

	/**
	 * 칸 하나를 배정한다. 냉장고 재료를 1개 이상 써야 하고(양이 있는 것), 쓰는 냉장고 재료마다 남은 양 안이어야 한다.
	 * 장부에 없는 냉장고 재료(기한 지남, 없는 id)를 쓰면 배정할 수 없다.
	 */
	private boolean tryClaim(List<SlotIngredient> ingredients) {
		boolean usesFridge = false;
		for (SlotIngredient ingredient : ingredients) {
			if (!ingredient.usesFridge() || !ingredient.isMeasured()) {
				continue;
			}
			Stock stock = stocks.get(ingredient.fridgeIngredientId());
			if (stock == null || !stock.canClaim(ingredient)) {
				return false;
			}
			stock.claim(ingredient);
			usesFridge = true;
		}
		return usesFridge;
	}

	private void claim(List<SlotIngredient> ingredients) {
		forEachMeasuredStock(ingredients, Stock::claim);
	}

	private void unclaim(List<SlotIngredient> ingredients) {
		forEachMeasuredStock(ingredients, Stock::unclaim);
	}

	private void forEachMeasuredStock(List<SlotIngredient> ingredients, StockAction action) {
		for (SlotIngredient ingredient : ingredients) {
			if (!ingredient.usesFridge() || !ingredient.isMeasured()) {
				continue;
			}
			Stock stock = stocks.get(ingredient.fridgeIngredientId());
			if (stock != null) {
				action.apply(stock, ingredient);
			}
		}
	}

	private StockLedger copy() {
		Map<Long, Stock> copied = new LinkedHashMap<>();
		stocks.forEach((id, stock) -> copied.put(id, stock.copy()));
		return new StockLedger(copied, allocated);
	}

	@FunctionalInterface
	private interface StockAction {
		void apply(Stock stock, SlotIngredient ingredient);
	}

	/**
	 * 재료 하나의 장부. claimed는 같은 종류 단위로 쓰기로 한 양의 합(기준 단위), wholeClaims는 통째로 쓴 칸 수다.
	 * 순서와 관계없이 더하고 뺄 수 있게 두 값을 따로 센다.
	 */
	private static final class Stock {

		private final FridgeStock source;
		private final BaseAmount base;
		private BigDecimal claimed;
		private int wholeClaims;

		private Stock(FridgeStock source, BaseAmount base, BigDecimal claimed, int wholeClaims) {
			this.source = source;
			this.base = base;
			this.claimed = claimed;
			this.wholeClaims = wholeClaims;
		}

		static Stock of(FridgeStock source) {
			return new Stock(source, StockUnits.toBase(source.amount(), source.unit()), BigDecimal.ZERO, 0);
		}

		FridgeStock source() {
			return source;
		}

		BigDecimal remaining() {
			if (wholeClaims > 0) {
				return BigDecimal.ZERO;
			}
			return base.amount().subtract(claimed).max(BigDecimal.ZERO);
		}

		boolean canClaim(SlotIngredient ingredient) {
			BigDecimal remaining = remaining();
			return sameDimension(ingredient)
					.map(need -> remaining.compareTo(need) >= 0)
					.orElse(remaining.signum() > 0);
		}

		void claim(SlotIngredient ingredient) {
			sameDimension(ingredient).ifPresentOrElse(need -> claimed = claimed.add(need), () -> wholeClaims++);
		}

		void unclaim(SlotIngredient ingredient) {
			sameDimension(ingredient).ifPresentOrElse(need -> claimed = claimed.subtract(need), () -> wholeClaims--);
		}

		/** 같은 종류 단위면 기준 단위로 바꾼 필요량, 계산할 수 없으면 비어 있다. */
		private Optional<BigDecimal> sameDimension(SlotIngredient ingredient) {
			return StockUnits.toBase(ingredient.amount(), ingredient.unit())
					.filter(need -> need.dimension() == base.dimension())
					.map(BaseAmount::amount);
		}

		Stock copy() {
			return new Stock(source, base, claimed, wholeClaims);
		}
	}
}
