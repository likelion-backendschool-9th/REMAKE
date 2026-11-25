package com.remake.recipe.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientUnit;

class UnitConversionTest {

	private static BigDecimal convert(String amount, RecipeUnit from, IngredientUnit to) {
		return UnitConversion.convert(new BigDecimal(amount), from, to).orElse(null);
	}

	@Test
	void 무게끼리와_부피끼리만_바꾼다() {
		assertThat(convert("150", RecipeUnit.G, IngredientUnit.KG)).isEqualByComparingTo("0.15");
		assertThat(convert("1.5", RecipeUnit.KG, IngredientUnit.G)).isEqualByComparingTo("1500");
		assertThat(convert("2", RecipeUnit.TBSP, IngredientUnit.ML)).isEqualByComparingTo("30");
		assertThat(convert("1", RecipeUnit.TSP, IngredientUnit.ML)).isEqualByComparingTo("5");
		assertThat(convert("1", RecipeUnit.CUP, IngredientUnit.L)).isEqualByComparingTo("0.2");
		assertThat(convert("300", RecipeUnit.G, IngredientUnit.ML)).isNull();
		assertThat(convert("1", RecipeUnit.TBSP, IngredientUnit.G)).isNull();
	}

	@Test
	void 개수_단위는_같은_단위끼리만_같다() {
		assertThat(convert("2", RecipeUnit.EA, IngredientUnit.EA)).isEqualByComparingTo("2");
		assertThat(convert("1", RecipeUnit.EA, IngredientUnit.PACK)).isNull();
		assertThat(convert("300", RecipeUnit.G, IngredientUnit.PACK)).isNull();
		assertThat(convert("1", RecipeUnit.BUNCH, IngredientUnit.EA)).isNull();
	}

	@Test
	void 꼬집과_약간은_바꾸지_않고_결과는_소수_셋째_자리에서_반올림한다() {
		assertThat(convert("1", RecipeUnit.PINCH, IngredientUnit.G)).isNull();
		assertThat(convert("1", RecipeUnit.TO_TASTE, IngredientUnit.G)).isNull();
		assertThat(UnitConversion.convert(null, RecipeUnit.G, IngredientUnit.G)).isEmpty();
		// 1/3컵 = 66.6ml → 0.0666L → 0.067L
		assertThat(convert("0.333", RecipeUnit.CUP, IngredientUnit.L)).isEqualByComparingTo("0.067");
	}
}
