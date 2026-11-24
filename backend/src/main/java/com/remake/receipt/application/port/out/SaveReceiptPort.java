package com.remake.receipt.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;

import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Command;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase.Item;
import com.remake.receipt.domain.ReceiptStoreCode;

public interface SaveReceiptPort {

	boolean lockOwnerAndCheckDuplicate(Long userId, Command command);

	Long saveReceipt(Long userId, LocalDate purchasedOn, LocalTime purchasedTime, String receiptNumber,
			String storeName, ReceiptStoreCode storeCode, String imageKey);

	Long saveItem(Long receiptId, Long categoryId, Item item);
}
