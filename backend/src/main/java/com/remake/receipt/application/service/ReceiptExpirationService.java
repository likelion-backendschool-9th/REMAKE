package com.remake.receipt.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.remake.receipt.application.port.in.ExpireOldReceiptsUseCase;
import com.remake.receipt.application.port.out.ReceiptHistoryPort;
import com.remake.receipt.application.port.out.ReceiptHistoryPort.StoredReceipt;
import com.remake.receipt.domain.ReceiptRetention;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class ReceiptExpirationService implements ExpireOldReceiptsUseCase {

	private final ReceiptHistoryPort receipts;
	private final ReceiptDeletionService deletion;
	private final Clock clock;

	@Override
	public int expireOldReceipts() {
		LocalDate cutoff = ReceiptRetention.earliestAllowedPurchaseDate(LocalDate.now(clock));
		List<StoredReceipt> expired = receipts.findPurchasedBefore(cutoff);
		int deleted = 0;
		for (StoredReceipt receipt : expired) {
			try {
				if (deletion.deleteIfPresent(receipt)) {
					deleted++;
				}
			}
			catch (RuntimeException failure) {
				log.warn("Failed to expire receipt {}", receipt.id(), failure);
			}
		}
		return deleted;
	}
}
