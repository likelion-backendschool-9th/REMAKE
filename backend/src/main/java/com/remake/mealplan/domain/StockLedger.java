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
 * 장부는 바꾸지 않고, 칸을 더할 때마다 새 장부를 돌려준다.
 *
 * <p>덮어쓰기는 장부가 모른다. 덮어써서 지울 칸은 부르는 쪽이 처음부터 배정 목록에서 빼고 넘긴다
 * (요청 기간 전체를 바꾸므로 그 칸들의 재료는 이번 생성이 모두 쓸 수 있다, #78 11-24).
 */
public final class StockLedger {

	private final Map<Long, Stock> stocks;

	private StockLedger(Map<Long, Stock> stocks) {
		this.stocks = stocks;
	}

	/**
	 * 냉장고 재료에서 남는 칸들의 배정량을 뺀 장부를 만든다.
	 *
	 * @param allocated 이번 생성 뒤에도 남는 칸들: 다른 식단표의 오늘 이후·요리 안 한 칸 중 덮어써서 지울 칸을 뺀 것
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
		StockLedger ledger = new StockLedger(stocks);
		for (AllocatedSlot slot : allocated) {
			ledger.claim(slot.ingredients());
		}
		return ledger;
	}

	/**
	 * 이번 생성에서 쓸 수 있는 냉장고 재료와 남은 양(냉장고 단위). 남은 양이 0이면 뺀다.
	 * AI에 보낼 재료 목록이고, 비어 있으면 하루도 만들 수 없다(MEAL_PLAN_NO_INGREDIENTS).
	 * 자르기도 같은 장부로 계산하므로 AI에 알려 준 양과 자르기의 기준이 같다.
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

	/** 그날 칸들을 배정한 새 장부. 칸 하나라도 배정할 수 없으면 비어 있다(장부는 그대로). */
	Optional<StockLedger> reserveDay(List<PlannedSlot> slots) {
		StockLedger next = copy();
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

	/** 이미 있는 칸의 배정량을 뺀다. 남은 양을 넘어도 그대로 뺀다(남은 양은 0 아래로 내려가지 않는다). */
	private void claim(List<SlotIngredient> ingredients) {
		for (SlotIngredient ingredient : ingredients) {
			if (!ingredient.usesFridge() || !ingredient.isMeasured()) {
				continue;
			}
			Stock stock = stocks.get(ingredient.fridgeIngredientId());
			if (stock != null) {
				stock.claim(ingredient);
			}
		}
	}

	private StockLedger copy() {
		Map<Long, Stock> copied = new LinkedHashMap<>();
		stocks.forEach((id, stock) -> copied.put(id, stock.copy()));
		return new StockLedger(copied);
	}

	/**
	 * 재료 하나의 장부. claimed는 같은 종류 단위로 쓰기로 한 양의 합(기준 단위), wholeClaims는 통째로 쓴 칸 수다.
	 * 통째로 쓴 칸이 하나라도 있으면 남은 양은 0이다.
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
