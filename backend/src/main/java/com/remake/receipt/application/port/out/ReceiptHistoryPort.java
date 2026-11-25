package com.remake.receipt.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.ReceiptHistoryUseCase.Row;
import com.remake.receipt.domain.ReceiptStoreCode;

public interface ReceiptHistoryPort {

	List<Row> list(Long userId, LocalDate from, LocalDate until);

	Optional<StoredReceipt> find(Long userId, Long id);

	List<StoredItem> items(Long receiptId);

	boolean delete(Long userId, Long id);

	/** 탈퇴 시 회원의 영수증을 모두 지우고, 지운 행의 image_key 목록을 돌려준다. */
	List<String> deleteAllForUser(Long userId);

	record StoredReceipt(Long id, ReceiptStoreCode storeCode, String storeName, LocalDate purchasedOn,
			String imageKey) {
	}

	record StoredItem(String name, Long categoryId, StorageLocation storage, BigDecimal amount,
			IngredientUnit unit, Long price) {
	}
}
