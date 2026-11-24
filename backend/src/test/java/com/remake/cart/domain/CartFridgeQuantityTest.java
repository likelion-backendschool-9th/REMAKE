package com.remake.cart.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.recipe.domain.RecipeUnit;

class CartFridgeQuantityTest {
	@ParameterizedTest
	@EnumSource(IngredientUnit.class)
	void 냉장고의_9개단위는_수량과단위를_그대로_보존한다(IngredientUnit unit) {
		var converted = CartFridgeQuantity.from(new BigDecimal("1.234"), RecipeUnit.valueOf(unit.name()));
		assertThat(converted.amount()).isEqualByComparingTo("1.234");
		assertThat(converted.unit()).isEqualTo(unit);
	}

	@ParameterizedTest
	@CsvSource({"TBSP,30", "TSP,10", "CUP,400"})
	void 계량단위만_ml로_환산한다(RecipeUnit unit, String expected) {
		var converted = CartFridgeQuantity.from(new BigDecimal("2"), unit);
		assertThat(converted.amount()).isEqualByComparingTo(expected);
		assertThat(converted.unit()).isEqualTo(IngredientUnit.ML);
	}


	@Test
	void 환산한_숫자가_DB범위를_넘으면_반올림하거나_잘라내지_않는다() {
		assertThatThrownBy(() -> CartFridgeQuantity.from(new BigDecimal("1000000"), RecipeUnit.CUP))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(CartFridgeQuantity.from(new BigDecimal("666666.6"), RecipeUnit.TBSP).amount())
				.isEqualByComparingTo("9999999");
	}

	@Test
	void 소수셋째자리_계량수량도_정확히_환산한다() {
		assertThat(CartFridgeQuantity.from(new BigDecimal("0.001"), RecipeUnit.TSP).amount())
				.isEqualByComparingTo("0.005");
	}

	@ParameterizedTest
	@EnumSource(value = RecipeUnit.class, names = {"PINCH", "TO_TASTE"})
	void 양을_셀수없는_단위는_이동하지않는다(RecipeUnit unit) {
		assertThatThrownBy(() -> CartFridgeQuantity.from(BigDecimal.ONE, unit)).isInstanceOf(IllegalArgumentException.class);
	}
}
