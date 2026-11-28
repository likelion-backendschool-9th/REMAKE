package com.remake.share.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.remake.share.application.port.in.RetrySharedRecipePhotoDeletionUseCase;
import com.remake.share.application.port.out.SharedRecipePhotoDeletionPort;
import com.remake.share.application.port.out.SharedRecipePhotoStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class SharedRecipePhotoDeletionService implements RetrySharedRecipePhotoDeletionUseCase {
	private final SharedRecipePhotoDeletionPort pending;
	private final SharedRecipePhotoStoragePort photos;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void deleteAfterCommit(String key) {
		if (pending.lock(key)) {
			deleteFile(key);
		}
	}

	@Override
	@Transactional
	public void retryPending() {
		pending.lockPending().forEach(this::deleteFile);
	}

	private void deleteFile(String key) {
		try {
			photos.delete(key);
			pending.complete(key);
		} catch (RuntimeException failure) {
			pending.defer(key);
			log.warn("공유 사진 삭제 재시도 대기: {}", key);
		}
	}
}
