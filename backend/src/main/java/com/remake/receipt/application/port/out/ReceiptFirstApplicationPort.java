package com.remake.receipt.application.port.out;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 영수증 번호가 있는 구매의 최초 냉장고·장바구니 반영 기록(#266).
 * 행이 있으면 이미 반영한 구매로 보고, 재등록 때 냉장고·장바구니를 건너뛴다.
 */
public interface ReceiptFirstApplicationPort {

	boolean exists(Long userId, String storeName, LocalDate purchasedOn, String receiptNumber);

	/** 이미 있으면 넣지 않는다. created_at은 최초 삽입만 유지한다. */
	void markIfAbsent(Long userId, String storeName, LocalDate purchasedOn, String receiptNumber,
			LocalTime purchasedTime);

	void deletePurchasedBefore(LocalDate cutoff);

	void deleteAllForUser(Long userId);
}
