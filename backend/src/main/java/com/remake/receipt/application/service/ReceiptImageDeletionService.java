package com.remake.receipt.application.service;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.remake.receipt.application.port.in.RetryReceiptImageDeletionUseCase;
import com.remake.receipt.application.port.out.ReceiptImageDeletionPort;
import com.remake.receipt.application.port.out.ReceiptImageStoragePort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class ReceiptImageDeletionService implements RetryReceiptImageDeletionUseCase {

	private final ReceiptImageDeletionPort pending;
	private final ReceiptImageStoragePort images;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteAfterCommit(String imageKey) {
		if (pending.lock(imageKey)) {
			deleteImage(imageKey);
		}
	}

	@Override
	@Transactional
	public void retryPending() {
		pending.lockPending().forEach(this::deleteImage);
	}

	private void deleteImage(String imageKey) {
		try {
			images.delete(imageKey);
			pending.complete(imageKey);
		} catch (IOException | IllegalArgumentException failure) {
			pending.defer(imageKey);
			log.warn("Failed to delete receipt image {}", imageKey, failure);
		}
	}
}
