package com.remake.share.adapter.in.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.remake.share.application.port.in.FinalizeWeeklyRankingUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class WeeklyRankingScheduler {
	private final FinalizeWeeklyRankingUseCase settlement;

	@Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
	void finalizePreviousWeek() {
		var result = settlement.finalizePreviousWeek();
		log.info("주간 순위 확정: week={}, finalized={}, ranks={}, rewards={}",
				result.weekStart(), result.finalized(), result.ranks(), result.rewards());
	}
}
