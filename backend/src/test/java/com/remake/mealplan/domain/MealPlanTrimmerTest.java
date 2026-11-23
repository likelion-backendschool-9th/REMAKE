package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;

// 도메인 규칙: Spring 없이 확인한다.
class MealPlanTrimmerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 25);
	private static final LocalDate FAR = TODAY.plusMonths(1);
	private static final long PORK = 1L;
	private static final long ONION = 2L;
	private static final long SOY = 3L;

	private static FridgeStock stock(long id, String amount, IngredientUnit unit) {
		return new FridgeStock(id, "재료" + id, new BigDecimal(amount), unit, FAR);
	}

	private static SlotIngredient fridge(long id, String amount, RecipeUnit unit) {
		return new SlotIngredient(id, "재료" + id, amount == null ? null : new BigDecimal(amount), unit);
	}

	private static SlotIngredient missing(String name, String amount, RecipeUnit unit) {
		return new SlotIngredient(null, name, amount == null ? null : new BigDecimal(amount), unit);
	}

	private static PlannedSlot slot(int day, MealType mealType, SlotIngredient... ingredients) {
		return new PlannedSlot(TODAY.plusDays(day - 1L), mealType, List.of(ingredients));
	}

	private static MealSchedule schedule(int days, Set<MealType> mealTypes) {
		return new MealSchedule(TODAY, days, mealTypes, Set.of());
	}

	private static StockLedger ledger(List<FridgeStock> stocks) {
		return StockLedger.of(stocks, List.of(), TODAY);
	}

	/** days일 동안 매 끼니 돼지고기 need g를 쓰는 칸들. */
	private static List<PlannedSlot> porkEveryMeal(int days, Set<MealType> mealTypes, String need) {
		List<PlannedSlot> slots = new ArrayList<>();
		for (int day = 1; day <= days; day++) {
			for (MealType mealType : mealTypes) {
				slots.add(slot(day, mealType, fridge(PORK, need, RecipeUnit.G)));
			}
		}
		return slots;
	}

	@Test
	void 재료가_넉넉하면_모든_날을_만든다() {
		Set<MealType> meals = EnumSet.of(MealType.LUNCH, MealType.DINNER);

		TrimResult result = MealPlanTrimmer.trim(schedule(3, meals), porkEveryMeal(3, meals, "100"),
				ledger(List.of(stock(PORK, "600", IngredientUnit.G))));

		assertThat(result.generatedDays()).isEqualTo(3);
		assertThat(result.hasNoDays()).isFalse();
	}

	@Test
	void 한_끼라도_모자란_날부터_그날_전체를_버리고_멈춘다() {
		Set<MealType> meals = EnumSet.allOf(MealType.class);

		// 하루 300g씩, 냉장고 800g: 3일째 점심(누적 800g)까지는 되지만 저녁에서 모자란다.
		TrimResult result = MealPlanTrimmer.trim(schedule(5, meals), porkEveryMeal(5, meals, "100"),
				ledger(List.of(stock(PORK, "800", IngredientUnit.G))));

		assertThat(result.generatedDays()).isEqualTo(2);
	}

	@Test
	void 뒤_날이_되더라도_건너뛰지_않는다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.DINNER, fridge(PORK, "100", RecipeUnit.G)),
				slot(2, MealType.DINNER, fridge(PORK, "500", RecipeUnit.G)),
				slot(3, MealType.DINNER, fridge(PORK, "100", RecipeUnit.G)));

		TrimResult result = MealPlanTrimmer.trim(schedule(3, meals), slots,
				ledger(List.of(stock(PORK, "300", IngredientUnit.G))));

		assertThat(result.generatedDays()).isEqualTo(1);
	}

	@Test
	void 첫날부터_못_만들면_0일이다() {
		TrimResult result = MealPlanTrimmer.trim(schedule(2, EnumSet.of(MealType.DINNER)),
				porkEveryMeal(2, EnumSet.of(MealType.DINNER), "500"),
				ledger(List.of(stock(PORK, "300", IngredientUnit.G))));

		assertThat(result.generatedDays()).isZero();
		assertThat(result.hasNoDays()).isTrue();
		assertThat(result.shortages()).isEmpty();
	}

	@Test
	void 냉장고_재료를_하나도_안_쓰는_칸은_만들_수_없다() {
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.DINNER, missing("소고기", "200", RecipeUnit.G)));

		TrimResult result = MealPlanTrimmer.trim(schedule(1, EnumSet.of(MealType.DINNER)), slots,
				ledger(List.of(stock(PORK, "300", IngredientUnit.G))));

		assertThat(result.generatedDays()).isZero();
	}

	@Test
	void 냉장고_재료를_약간이나_꼬집으로만_쓰면_냉장고_재료를_쓴_것으로_보지_않는다() {
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.DINNER, fridge(SOY, null, RecipeUnit.TO_TASTE), fridge(SOY, "1", RecipeUnit.PINCH)));

		TrimResult result = MealPlanTrimmer.trim(schedule(1, EnumSet.of(MealType.DINNER)), slots,
				ledger(List.of(stock(SOY, "500", IngredientUnit.ML))));

		assertThat(result.generatedDays()).isZero();
	}

	@Test
	void 같은_종류_단위는_환산해서_계산한다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		// 간장 0.1L = 100ml, 하루 3큰술 = 45ml → 2일까지
		List<PlannedSlot> slots = new ArrayList<>();
		for (int day = 1; day <= 3; day++) {
			slots.add(slot(day, MealType.DINNER, fridge(SOY, "3", RecipeUnit.TBSP)));
		}

		TrimResult result = MealPlanTrimmer.trim(schedule(3, meals), slots,
				ledger(List.of(stock(SOY, "0.1", IngredientUnit.L))));

		assertThat(result.generatedDays()).isEqualTo(2);
	}

	@Test
	void 단위_종류가_다르면_그_칸이_재료를_통째로_쓴다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		// 냉장고 삼겹살 2팩, 레시피 300g: 팩과 g는 바꿀 수 없어 첫 끼가 2팩을 다 쓴다.
		TrimResult result = MealPlanTrimmer.trim(schedule(2, meals), porkEveryMeal(2, meals, "300"),
				ledger(List.of(stock(PORK, "2", IngredientUnit.PACK))));

		assertThat(result.generatedDays()).isEqualTo(1);
	}

	@Test
	void 개수_단위는_같은_단위끼리만_계산한다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		List<PlannedSlot> sameUnit = List.of(
				slot(1, MealType.DINNER, fridge(ONION, "1", RecipeUnit.EA)),
				slot(2, MealType.DINNER, fridge(ONION, "1", RecipeUnit.EA)));
		List<PlannedSlot> otherUnit = List.of(
				slot(1, MealType.DINNER, fridge(ONION, "1", RecipeUnit.PACK)),
				slot(2, MealType.DINNER, fridge(ONION, "1", RecipeUnit.PACK)));
		List<FridgeStock> onions = List.of(stock(ONION, "2", IngredientUnit.EA));

		assertThat(MealPlanTrimmer.trim(schedule(2, meals), sameUnit, ledger(onions)).generatedDays()).isEqualTo(2);
		assertThat(MealPlanTrimmer.trim(schedule(2, meals), otherUnit, ledger(onions)).generatedDays()).isEqualTo(1);
	}

	@Test
	void 한_칸에서_같은_재료를_여러_줄로_쓰면_합쳐서_확인한다() {
		List<PlannedSlot> slots = List.of(slot(1, MealType.DINNER,
				fridge(PORK, "200", RecipeUnit.G), fridge(PORK, "200", RecipeUnit.G)));

		TrimResult result = MealPlanTrimmer.trim(schedule(1, EnumSet.of(MealType.DINNER)), slots,
				ledger(List.of(stock(PORK, "300", IngredientUnit.G))));

		assertThat(result.generatedDays()).isZero();
	}

	@Test
	void 다른_식단표가_배정한_양은_빼고_계산한다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		// 다른 식단표(이번 기간 밖)의 칸이 200g을 쓰기로 했다. 냉장고 400g → 남은 200g → 하루 100g씩 2일
		AllocatedSlot other = new AllocatedSlot(TODAY.plusDays(10), List.of(fridge(PORK, "200", RecipeUnit.G)));
		StockLedger ledger = StockLedger.of(List.of(stock(PORK, "400", IngredientUnit.G)), List.of(other), TODAY);

		TrimResult result = MealPlanTrimmer.trim(schedule(3, meals), porkEveryMeal(3, meals, "100"), ledger);

		assertThat(result.generatedDays()).isEqualTo(2);
	}

	@Test
	void 덮어쓸_날의_예전_배정량은_그날을_만들_때_돌려받는다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		// 예전 식단표가 1~3일에 매일 100g. 냉장고 300g이라 남은 양은 0이지만,
		// 하루씩 만들 때 그날 예전 칸의 100g을 돌려받아 새 칸에 쓴다.
		List<AllocatedSlot> old = List.of(
				new AllocatedSlot(TODAY, List.of(fridge(PORK, "100", RecipeUnit.G))),
				new AllocatedSlot(TODAY.plusDays(1), List.of(fridge(PORK, "100", RecipeUnit.G))),
				new AllocatedSlot(TODAY.plusDays(2), List.of(fridge(PORK, "100", RecipeUnit.G))));
		StockLedger ledger = StockLedger.of(List.of(stock(PORK, "300", IngredientUnit.G)), old, TODAY);

		TrimResult result = MealPlanTrimmer.trim(schedule(3, meals), porkEveryMeal(3, meals, "100"), ledger);

		assertThat(ledger.available()).isEmpty();
		assertThat(result.generatedDays()).isEqualTo(3);
	}

	@Test
	void 못_만든_날의_예전_배정량은_돌려받지_않는다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		// 예전 식단표 2일째 칸이 200g. 새 칸은 1일 150g, 2일 300g. 냉장고 400g.
		// 1일: 남은 200g 안에서 150g → 됨. 2일: 예전 200g을 돌려받아도 남은 250g < 300g → 멈춤.
		List<AllocatedSlot> old = List.of(
				new AllocatedSlot(TODAY.plusDays(1), List.of(fridge(PORK, "200", RecipeUnit.G))));
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.DINNER, fridge(PORK, "150", RecipeUnit.G)),
				slot(2, MealType.DINNER, fridge(PORK, "300", RecipeUnit.G)));
		StockLedger ledger = StockLedger.of(List.of(stock(PORK, "400", IngredientUnit.G)), old, TODAY);

		TrimResult result = MealPlanTrimmer.trim(schedule(2, meals), slots, ledger);

		assertThat(result.generatedDays()).isEqualTo(1);
	}

	@Test
	void 소비기한이_지난_재료는_쓰지_않고_오늘까지인_재료는_쓴다() {
		FridgeStock expired = new FridgeStock(PORK, "돼지고기", new BigDecimal("500"), IngredientUnit.G,
				TODAY.minusDays(1));
		FridgeStock dDay = new FridgeStock(ONION, "양파", new BigDecimal("2"), IngredientUnit.EA, TODAY);
		StockLedger ledger = ledger(List.of(expired, dDay));
		MealSchedule oneDinner = schedule(1, EnumSet.of(MealType.DINNER));

		assertThat(ledger.available()).extracting(FridgeStock::ingredientId).containsExactly(ONION);
		assertThat(MealPlanTrimmer.trim(oneDinner,
				List.of(slot(1, MealType.DINNER, fridge(PORK, "100", RecipeUnit.G))), ledger).generatedDays())
				.isZero();
		assertThat(MealPlanTrimmer.trim(oneDinner,
				List.of(slot(1, MealType.DINNER, fridge(ONION, "1", RecipeUnit.EA))), ledger).generatedDays())
				.isEqualTo(1);
	}

	@Test
	void 장바구니에는_남긴_날의_냉장고에_없는_재료만_담고_약간과_꼬집은_뺀다() {
		Set<MealType> meals = EnumSet.of(MealType.DINNER);
		SlotIngredient greenOnion = missing("대파", "1", RecipeUnit.BUNCH);
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.DINNER, fridge(PORK, "200", RecipeUnit.G), greenOnion,
						missing("소금", null, RecipeUnit.TO_TASTE), missing("후추", "1", RecipeUnit.PINCH)),
				slot(2, MealType.DINNER, fridge(PORK, "200", RecipeUnit.G), missing("마늘", "10", RecipeUnit.G)));

		TrimResult result = MealPlanTrimmer.trim(schedule(2, meals), slots,
				ledger(List.of(stock(PORK, "300", IngredientUnit.G))));

		assertThat(result.generatedDays()).isEqualTo(1);
		assertThat(result.shortages()).containsExactly(
				new TrimResult.Shortage(new MealSlotKey(TODAY, MealType.DINNER), greenOnion));
	}

	@Test
	void 남기는_칸은_채우지_않고_남기는_칸만_있는_날도_만든_날로_센다() {
		// 오늘 아침·점심은 이미 요리해서 남긴다. 오늘은 저녁만, 내일은 모든 끼니를 채운다.
		Set<MealType> meals = EnumSet.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);
		Set<MealSlotKey> kept = Set.of(
				new MealSlotKey(TODAY, MealType.BREAKFAST), new MealSlotKey(TODAY, MealType.LUNCH));
		MealSchedule schedule = new MealSchedule(TODAY, 2, meals, kept);
		List<PlannedSlot> slots = new ArrayList<>(List.of(slot(1, MealType.DINNER, fridge(PORK, "100", RecipeUnit.G))));
		for (MealType mealType : meals) {
			slots.add(slot(2, mealType, fridge(PORK, "100", RecipeUnit.G)));
		}

		TrimResult result = MealPlanTrimmer.trim(schedule, slots, ledger(List.of(stock(PORK, "400", IngredientUnit.G))));

		assertThat(schedule.mealTypesOn(TODAY)).containsExactly(MealType.DINNER);
		assertThat(result.generatedDays()).isEqualTo(2);

		MealSchedule allKept = new MealSchedule(TODAY, 1, EnumSet.of(MealType.BREAKFAST),
				Set.of(new MealSlotKey(TODAY, MealType.BREAKFAST)));
		assertThat(MealPlanTrimmer.trim(allKept, List.of(), ledger(List.of(stock(PORK, "1", IngredientUnit.G))))
				.generatedDays()).isEqualTo(1);
	}

	@Test
	void 요청한_끼니가_빠진_날에서_멈춘다() {
		Set<MealType> meals = EnumSet.of(MealType.LUNCH, MealType.DINNER);
		List<PlannedSlot> slots = List.of(
				slot(1, MealType.LUNCH, fridge(PORK, "100", RecipeUnit.G)),
				slot(1, MealType.DINNER, fridge(PORK, "100", RecipeUnit.G)),
				slot(2, MealType.LUNCH, fridge(PORK, "100", RecipeUnit.G)));

		TrimResult result = MealPlanTrimmer.trim(schedule(2, meals), slots,
				ledger(List.of(stock(PORK, "1000", IngredientUnit.G))));

		assertThat(result.generatedDays()).isEqualTo(1);
	}

	@Test
	void 쓸_수_있는_재료는_남은_양을_냉장고_단위로_돌려주고_다_쓴_재료는_뺀다() {
		List<AllocatedSlot> allocated = List.of(new AllocatedSlot(TODAY.plusDays(3), List.of(
				fridge(PORK, "300", RecipeUnit.G), fridge(ONION, "1", RecipeUnit.PACK))));
		StockLedger ledger = StockLedger.of(
				List.of(stock(PORK, "1", IngredientUnit.KG), stock(ONION, "2", IngredientUnit.EA)), allocated, TODAY);

		assertThat(ledger.available()).singleElement().satisfies(stock -> {
			assertThat(stock.ingredientId()).isEqualTo(PORK);
			assertThat(stock.amount()).isEqualByComparingTo("0.7");
			assertThat(stock.unit()).isEqualTo(IngredientUnit.KG);
		});
	}
}
