package com.remake.receipt.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.application.port.in.GetIngredientCategoriesUseCase;
import com.remake.ingredient.application.port.in.ResolveIngredientUnitUseCase;
import com.remake.ingredient.domain.IngredientCatalogItem;
import com.remake.ingredient.domain.IngredientCategory;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

class ReceiptImageAnalysisServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T00:00:00Z"), ZoneId.of("Asia/Seoul"));
	private final EventTracker events = mock(EventTracker.class);
	private final GetIngredientCatalogUseCase catalog = mock(GetIngredientCatalogUseCase.class);
	private final GetIngredientCategoriesUseCase categories = mock(GetIngredientCategoriesUseCase.class);
	private final ResolveIngredientUnitUseCase ingredientUnits = mock(ResolveIngredientUnitUseCase.class);
	private static final Long USER_ID = 1L;
	@Test
	void 같은_OCR_출력에서_판매처와_구매일과_품목을_읽는다() {
		byte[] image = {1, 2, 3};
		when(catalog.findCatalogItemByName("우유")).thenReturn(Optional.of(
				new IngredientCatalogItem(1L, 2L, "우유", IngredientUnit.EA, 7, false)));
		when(ingredientUnits.resolveUnit("우유", IngredientCategoryCode.DAIRY_EGG)).thenReturn(IngredientUnit.PACK);
		when(categories.getIngredientCategory(2L)).thenReturn(new IngredientCategory(2L,
				IngredientCategoryCode.DAIRY_EGG, "유제품·달걀", null, StorageLocation.FRIDGE, 6));
		var service = new ReceiptImageAnalysisService((input, timeout) -> {
			assertThat(input).isSameAs(image);
			assertThat(timeout).isEqualTo(Duration.ofSeconds(30));
			return """
					상호: 테스트 마트
					판매일 26-11-16
					품목명 수량 금액
					우유 2개 5,000
					수세미 1개 2,000
					""";
		}, events, CLOCK, catalog, categories, ingredientUnits, (userId, key) -> false);

		var result = service.analyze(USER_ID, image);
		assertThat(result.header().storeName()).isEqualTo("테스트 마트");
		assertThat(result.header().purchasedOn()).isEqualTo(LocalDate.of(2026, 11, 16));
		assertThat(result.items()).singleElement().satisfies(item -> {
			assertThat(item.source().name()).isEqualTo("우유");
			assertThat(item.source().totalPrice()).isEqualTo(5000);
			assertThat(item.unit()).isEqualTo(IngredientUnit.PACK);
		});
		verify(events).track(eq(EventName.RECEIPT_SCAN_SUCCEEDED), eq(Screen.RECEIPT_SCAN),
				argThat(properties -> properties.keySet().equals(Set.of("item_count", "food_item_count", "duration_ms"))
						&& properties.get("item_count").equals(2)
						&& properties.get("food_item_count").equals(1)
						&& (long) properties.get("duration_ms") >= 0),
				eq(EventTrackCommand.ofUser(USER_ID)));
		verifyNoMoreInteractions(events);
	}

	@Test
	void 읽을_수_있는_품목이_없으면_인식_실패를_반환한다() {
		var service = new ReceiptImageAnalysisService((image, timeout) -> "상호: 테스트 마트", events, CLOCK, catalog, categories, ingredientUnits, (userId, key) -> false);

		assertThatThrownBy(() -> service.analyze(USER_ID, new byte[0]))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RECEIPT_NOT_RECOGNIZED));
		verify(events).track(eq(EventName.RECEIPT_SCAN_FAILED), eq(Screen.RECEIPT_SCAN),
				argThat(properties -> properties.keySet().equals(Set.of("error_code", "duration_ms"))
						&& properties.get("error_code").equals("RECEIPT_NOT_RECOGNIZED")
						&& (long) properties.get("duration_ms") >= 0),
				eq(EventTrackCommand.ofUser(USER_ID)));
		verifyNoMoreInteractions(events);
	}
}
