package com.remake.receipt.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;

class ReceiptCreateRequestTest {
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	@Test
	void 구매일_누락과_판매처_공백을_거절한다() {
		var request = new ReceiptCreateRequest(null, " ", null, null, null, List.of(item("0.125")));
		assertEquals(Set.of("purchasedOn", "storeName"), invalidFields(request));
	}

	@Test
	void 미래_구매일과_품목_내부의_잘못된_값을_검출한다() {
		var request = new ReceiptCreateRequest(LocalDate.of(2026, 11, 21), "동네마트", null, null, null,
			List.of(item("0.0001")));
		assertEquals(Set.of("purchasedOn", "items[0].amount"), invalidFields(request));
	}

	private Set<String> invalidFields(ReceiptCreateRequest request) {
		try (var factory = Validation.byDefaultProvider().configure().clockProvider(() -> CLOCK)
			.buildValidatorFactory()) {
			return factory.getValidator().validate(request).stream()
				.map(violation -> violation.getPropertyPath().toString())
				.collect(Collectors.toSet());
		}
	}

	private ReceiptCreateRequest.Item item(String amount) {
		return new ReceiptCreateRequest.Item("양파", IngredientCategoryCode.VEGETABLE, new BigDecimal(amount),
			null, StorageLocation.PANTRY, null, null, null, null);
	}
}
