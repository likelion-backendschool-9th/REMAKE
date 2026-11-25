package com.remake.receipt.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.receipt.application.port.in.DeleteReceiptsForUserUseCase;
import com.remake.receipt.application.port.out.ReceiptHistoryPort;
import com.remake.receipt.application.port.out.ReceiptImageDeletionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 탈퇴 시 영수증 행을 지우고, 사진은 #238과 같이 큐 + afterCommit으로 지운다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class DeleteReceiptsForUserService implements DeleteReceiptsForUserUseCase {

	private final ReceiptHistoryPort receipts;
	private final ReceiptImageDeletionPort pendingImages;
	private final ReceiptImageDeletionService imageDeletion;

	@Override
	@Transactional
	public void deleteReceiptsForUser(Long userId) {
		List<String> imageKeys = receipts.deleteAllForUser(userId);
		for (String imageKey : imageKeys) {
			if (imageKey == null || imageKey.isBlank()) {
				continue;
			}
			pendingImages.enqueue(imageKey);
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					try {
						imageDeletion.deleteAfterCommit(imageKey);
					} catch (RuntimeException failure) {
						log.warn("탈퇴 영수증 사진 정리가 큐에 남음 key={}", imageKey, failure);
					}
				}
			});
		}
	}
}
