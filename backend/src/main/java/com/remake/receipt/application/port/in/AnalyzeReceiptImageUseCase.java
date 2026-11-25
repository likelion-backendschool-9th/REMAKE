package com.remake.receipt.application.port.in;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.remake.receipt.domain.OcrReceiptHeader;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.domain.OcrReceiptItemParser.Item;

public interface AnalyzeReceiptImageUseCase {

	Result analyze(Long userId, byte[] image);

	record Result(OcrReceiptHeader header, List<ScannedItem> items) {
	}
	record ScannedItem(Item source, String name, IngredientCategoryCode categoryCode, StorageLocation storage,
			LocalDate expirationDate, BigDecimal amount, IngredientUnit unit) {
	}
}
