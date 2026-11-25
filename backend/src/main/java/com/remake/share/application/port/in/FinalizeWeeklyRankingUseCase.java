package com.remake.share.application.port.in;

import java.time.LocalDate;

/** 지난주 순위·보상을 한 번만 확정한다. 공개/관리자 실행 API는 제공하지 않는다. */
public interface FinalizeWeeklyRankingUseCase {
	Result finalizePreviousWeek();

	record Result(LocalDate weekStart, boolean finalized, int ranks, int rewards) { }
}
