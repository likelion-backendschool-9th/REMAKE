package com.remake.receipt.application.port.in;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.domain.ReceiptStoreCode;

public interface RegisterReceiptUseCase {

	Result register(Long userId, InputStream image, Command command);

	record Command(LocalDate purchasedOn, String storeName, ReceiptStoreCode storeCode,
			LocalTime purchasedTime, String receiptNumber, List<ItemRequest> items) {
	}

	record Item(String name, IngredientCategoryCode categoryCode, BigDecimal amount, IngredientUnit unit, Long price,
			StorageLocation storage, LocalDate expirationDate) {
	}

	record ItemRequest(String name, IngredientCategoryCode categoryCode, BigDecimal amount, Long price,
			StorageLocation storage, LocalDate expirationDate,
			BigDecimal purchaseQuantity, Volume volume, List<CartDeduction> cartDeductions) {
	}

	record Volume(BigDecimal amount, IngredientUnit unit, VolumeScope scope) {
	}

	enum VolumeScope {
		PER_PACK, TOTAL
	}

	record CartDeduction(Long cartItemId, BigDecimal amount) {
	}

	record Result(Long receiptId, int registeredCount, int cartRemovedCount) {
	}
}
