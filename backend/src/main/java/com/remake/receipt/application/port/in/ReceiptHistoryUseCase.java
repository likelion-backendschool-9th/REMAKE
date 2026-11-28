package com.remake.receipt.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.domain.ReceiptStoreCode;

import io.swagger.v3.oas.annotations.media.Schema;

public interface ReceiptHistoryUseCase {

	MonthlyReceipts list(Long userId, YearMonth month);

	Detail detail(Long userId, Long id);

	byte[] image(Long userId, Long id);

	void delete(Long userId, Long id);

	record MonthlyReceipts(YearMonth month, long totalAmount, @Schema(nullable = true) Long averageDiffAmount,
			List<Week> weeks) {
	}

	@Schema(name = "ReceiptWeek")
	record Week(String label, LocalDate startDate, LocalDate endDate, long totalAmount, List<Row> receipts) {
	}

	@Schema(name = "ReceiptRow")
	record Row(Long id, ReceiptStoreCode storeCode, String storeName, LocalDate purchasedOn,
			long itemCount, long totalAmount) {
	}

	record Detail(Row receipt, List<Item> items) {
	}

	@Schema(name = "ReceiptDetailItem")
	record Item(String name, IngredientCategoryCode categoryCode, String imageUrl,
			StorageLocation storage, BigDecimal amount, IngredientUnit unit, @Schema(nullable = true) Long price) {
	}
}
