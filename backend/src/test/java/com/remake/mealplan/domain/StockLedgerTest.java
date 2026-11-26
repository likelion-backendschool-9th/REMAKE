package com.remake.mealplan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;

class StockLedgerTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Test
	void 남은_양_목록은_배정한_양을_빼고_카테고리를_그대로_둔다() {
		FridgeStock pork = new FridgeStock(1L, "돼지고기 앞다리", new BigDecimal("600"), IngredientUnit.G,
				TODAY.plusDays(4), IngredientCategoryCode.MEAT);
		AllocatedSlot used = new AllocatedSlot(TODAY,
				List.of(new SlotIngredient(1L, "돼지고기 앞다리", new BigDecimal("200"), RecipeUnit.G)));

		List<FridgeStock> available = StockLedger.of(List.of(pork), List.of(used), TODAY).available();

		assertThat(available).singleElement().satisfies(stock -> {
			assertThat(stock.amount()).isEqualByComparingTo("400");
			// 식약처 후보가 "돼지고기 앞다리"를 사전 이름 "돼지고기"에 맞추려면 카테고리가 필요하다(#145 규칙)
			assertThat(stock.categoryCode()).isEqualTo(IngredientCategoryCode.MEAT);
		});
	}
}
