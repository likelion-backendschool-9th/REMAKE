package com.remake.share.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.remake.share.application.port.in.RetrySharedRecipePhotoDeletionUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class SharedRecipePhotoDeletionScheduler {
	private final RetrySharedRecipePhotoDeletionUseCase deletion;

	@Scheduled(fixedDelayString = "PT1M")
	void retryPending() {
		deletion.retryPending();
	}
}
