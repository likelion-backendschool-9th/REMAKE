package com.remake.receipt.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.domain.IngredientUnit;

class OcrQuantityTest {

	@ParameterizedTest
	@CsvSource({"병,PACK", "마리,EA", "단,BUNCH", "쪽,PIECE", "장,SHEET", "kg,KG", "mL,ML"})
	void 영수증_단위를_냉장고_단위로_바꾼다(String input, IngredientUnit expected) {
		BigDecimal amount = new BigDecimal("1.250");
		assertThat(OcrQuantity.normalize(amount, input)).isEqualTo(new OcrQuantity(amount, expected));
	}

	@Test
	void 수량을_읽지_못하면_단위와_함께_한_개로_처리한다() {
		assertThat(OcrQuantity.normalize(null, "kg")).isEqualTo(new OcrQuantity(BigDecimal.ONE, IngredientUnit.EA));
	}

	@Test
	void 냉장고에_등록할_수_없는_단위는_거절한다() {
		assertThatThrownBy(() -> OcrQuantity.normalize(BigDecimal.ONE, "TBSP"))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
	}
}
