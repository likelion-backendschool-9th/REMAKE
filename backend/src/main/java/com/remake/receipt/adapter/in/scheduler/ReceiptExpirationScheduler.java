package com.remake.receipt.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.receipt.application.port.in.ExpireOldReceiptsUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 새벽 3시(한국 시간) 구매일 기준 6개월을 넘긴 영수증 정리. */
@Slf4j
@Component
@RequiredArgsConstructor
class ReceiptExpirationScheduler {

	private final ExpireOldReceiptsUseCase expireOldReceiptsUseCase;

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	void expireOldReceipts() {
		int deleted = expireOldReceiptsUseCase.expireOldReceipts();
		log.info("오래된 영수증 정리 완료 deleted={}", deleted);
	}
}
