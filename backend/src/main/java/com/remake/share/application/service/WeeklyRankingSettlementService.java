package com.remake.share.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.quota.application.port.in.GrantWeeklyRecipeRewardUseCase;
import com.remake.share.application.port.in.FinalizeWeeklyRankingUseCase;
import com.remake.share.application.port.out.ManageSharedRecipePort;
import com.remake.share.application.port.out.WeeklyRankingSettlementPort;
import com.remake.share.domain.SharedRecipe;
import com.remake.share.domain.SharedRecipeRankingPolicy;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class WeeklyRankingSettlementService implements FinalizeWeeklyRankingUseCase {
	private final WeeklyRankingSettlementPort settlements;
	private final ManageSharedRecipePort posts;
	private final GrantWeeklyRecipeRewardUseCase rewards;
	private final Clock clock;

	@Override
	@Transactional
	public Result finalizePreviousWeek() {
		// 삭제와 확정의 선후는 이 잠금을 획득한 트랜잭션의 커밋으로 결정한다.
		// 주 완료 기록을 먼저 잠그면 삭제와의 잠금 순서가 뒤집히므로 반드시 첫 DB 접근이다.
		posts.lockDeletionAndSettlement();
		LocalDateTime end = SharedRecipe.weekStart(clock);
		var week = end.toLocalDate().minusWeeks(1);
		LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Seoul")));
		if (!settlements.claim(week, now)) {
			return new Result(week, false, 0, 0);
		}
		var ranks = settlements.rank(week.atStartOfDay(), end);
		int granted = 0;
		for (var rank : ranks) {
			settlements.save(week, rank, now);
			if (rank.rank() <= SharedRecipeRankingPolicy.TOP_LIMIT
					&& rewards.grant(rank.authorId(), rank.sharedRecipeId(), week, rank.rank())) {
				granted++;
			}
		}
		return new Result(week, true, ranks.size(), granted);
	}
}
