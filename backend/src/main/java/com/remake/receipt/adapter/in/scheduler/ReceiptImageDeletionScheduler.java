package com.remake.receipt.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.receipt.application.port.in.RetryReceiptImageDeletionUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ReceiptImageDeletionScheduler {

	private final RetryReceiptImageDeletionUseCase deletion;

	@Scheduled(fixedDelayString = "PT1M")
	void retryPending() {
		deletion.retryPending();
	}
}
