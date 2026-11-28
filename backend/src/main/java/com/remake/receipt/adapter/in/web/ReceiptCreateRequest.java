package com.remake.receipt.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.util.StringUtils;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.VolumeScope;
import com.remake.receipt.domain.ReceiptStoreCode;

/** 최종 등록 multipart 요청에서 사진과 함께 받는 payload JSON이다. */
public record ReceiptCreateRequest(
	// OCR이 놓친 값은 확인 화면에서 직접 입력한 뒤 최종 등록한다.
	@NotNull @PastOrPresent LocalDate purchasedOn,
	@NotBlank String storeName,
	ReceiptStoreCode storeCode,
	LocalTime purchasedTime,
	String receiptNumber,
	@NotEmpty List<@NotNull @Valid Item> items
) {

	RegisterReceiptUseCase.Command toCommand() {
		String number = StringUtils.hasText(receiptNumber) ? receiptNumber.strip() : null;
		return new RegisterReceiptUseCase.Command(purchasedOn, storeName, storeCode, purchasedTime, number, items.stream()
				.map(Item::toItemRequest)
				.toList());
	}

	@Schema(name = "ReceiptCreateItem")
	public record Item(
		@NotBlank @Size(max = 50) String name,
		@NotNull IngredientCategoryCode categoryCode,
		@Positive @Digits(integer = 7, fraction = 3) BigDecimal amount,
		@PositiveOrZero Long price,
		@NotNull StorageLocation storage,
		LocalDate expirationDate,
		@Positive @Digits(integer = 7, fraction = 3) BigDecimal purchaseQuantity,
		@Valid Volume volume,
		@Size(max = 1) List<@NotNull @Valid CartDeduction> cartDeductions
	) {
		RegisterReceiptUseCase.ItemRequest toItemRequest() {
			var deductions = cartDeductions == null ? List.<RegisterReceiptUseCase.CartDeduction>of()
					: cartDeductions.stream().map(entry -> new RegisterReceiptUseCase.CartDeduction(
							entry.cartItemId(), entry.amount())).toList();
			return new RegisterReceiptUseCase.ItemRequest(name, categoryCode, amount, price, storage, expirationDate,
					purchaseQuantity,
					volume == null ? null : new RegisterReceiptUseCase.Volume(volume.amount(), volume.unit(), volume.scope()),
					deductions);
		}
	}

	@Schema(name = "ReceiptCreateVolume")
	public record Volume(
		@NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal amount,
		@NotNull IngredientUnit unit,
		@NotNull VolumeScope scope
	) {
	}

	@Schema(name = "ReceiptCartDeduction")
	public record CartDeduction(
		@NotNull @Positive Long cartItemId,
		@NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal amount
	) {
	}
}
