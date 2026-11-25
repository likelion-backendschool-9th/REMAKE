package com.remake.receipt.application.service;

import org.springframework.stereotype.Service;

import com.remake.receipt.application.port.in.DeleteReceiptsForUserUseCase;

import lombok.RequiredArgsConstructor;

/** 탈퇴 시 영수증·사진 정리는 {@link ReceiptDeletionService}에 위임한다. */
@Service
@RequiredArgsConstructor
class DeleteReceiptsForUserService implements DeleteReceiptsForUserUseCase {

	private final ReceiptDeletionService deletion;

	@Override
	public void deleteReceiptsForUser(Long userId) {
		deletion.deleteAllForUser(userId);
	}
}
