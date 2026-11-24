package com.remake.receipt.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface FindDuplicateReceiptPort {

	boolean exists(Long userId, Key key);

	record Key(String storeName, LocalDate purchasedOn, LocalTime purchasedTime,
			String receiptNumber, List<String> itemNames) {

		public boolean hasRequiredFields() {
			return storeName != null && !storeName.isBlank() && purchasedOn != null;
		}
	}
}
