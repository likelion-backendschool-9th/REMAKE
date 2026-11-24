package com.remake.user.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.user.application.port.in.PurgeExpiredDeletionCodesUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 새벽(한국 시간) 생성 후 하루 지난 계정 삭제 인증번호 요청을 지운다. */
@Slf4j
@Component
@RequiredArgsConstructor
class AccountDeletionCodeCleanupScheduler {

	private final PurgeExpiredDeletionCodesUseCase purgeExpiredDeletionCodesUseCase;

	@Scheduled(cron = "0 20 3 * * *", zone = "Asia/Seoul")
	void purgeExpiredCodes() {
		int deleted = purgeExpiredDeletionCodesUseCase.purgeExpired();
		if (deleted > 0) {
			log.info("만료 계정 삭제 인증번호 {}건을 정리했습니다", deleted);
		}
	}
}
