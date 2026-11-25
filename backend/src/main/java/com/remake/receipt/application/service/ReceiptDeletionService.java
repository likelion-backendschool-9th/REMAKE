package com.remake.receipt.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.receipt.application.port.out.ReceiptHistoryPort;
import com.remake.receipt.application.port.out.ReceiptHistoryPort.StoredReceipt;
import com.remake.receipt.application.port.out.ReceiptImageDeletionPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 영수증 DB 삭제와 이미지 삭제 재시도 대기(#238)를 한곳에서 처리한다.
 * 사용자 삭제와 6개월 만료 배치가 같은 순서를 쓴다: DB 삭제 → 대기 테이블 → 커밋 후 파일 삭제.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ReceiptDeletionService {

	private final ReceiptHistoryPort receipts;
	private final ReceiptImageDeletionPort pendingImages;
	private final ReceiptImageDeletionService imageDeletion;

	@Transactional
	public void deleteOwned(Long userId, Long id) {
		StoredReceipt receipt = receipts.find(userId, id)
				.orElseThrow(() -> new BusinessException(ErrorCode.RECEIPT_NOT_FOUND));
		if (!receipts.delete(userId, id)) {
			throw new BusinessException(ErrorCode.RECEIPT_NOT_FOUND);
		}
		queueImageDeletion(id, receipt.imageKey());
	}

	/**
	 * 만료 배치용. 이미 없으면 false. 영수증마다 새 트랜잭션으로 부분 실패가 전체를 되돌리지 않게 한다.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean deleteIfPresent(StoredReceipt receipt) {
		if (!receipts.deleteById(receipt.id())) {
			return false;
		}
		queueImageDeletion(receipt.id(), receipt.imageKey());
		return true;
	}

	private void queueImageDeletion(Long receiptId, String imageKey) {
		pendingImages.enqueue(imageKey);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					imageDeletion.deleteAfterCommit(imageKey);
				}
				catch (RuntimeException failure) {
					log.warn("Receipt image cleanup remains queued for {}", receiptId, failure);
				}
			}
		});
	}
}
