package com.remake.share.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface WeeklyRankingSettlementPort {
	/** 같은 주 동시 실행은 DB 고유 키에서 기다린 뒤 하나만 성공한다. 롤백 시 재시도 가능. */
	boolean claim(LocalDate weekStart, LocalDateTime now);
	List<Rank> rank(LocalDateTime start, LocalDateTime end);
	void save(LocalDate weekStart, Rank rank, LocalDateTime now);

	record Rank(Long sharedRecipeId, Long authorId, int rank, int saveCount) { }
}
