package com.remake.quota.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.quota.application.port.in.GrantWeeklyRecipeRewardUseCase;
import com.remake.quota.application.port.out.RecipeRewardPort;
import com.remake.user.application.port.in.GetUserUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class WeeklyRecipeRewardService implements GrantWeeklyRecipeRewardUseCase {
	/** 제안: 탈퇴자에게 보상을 지급하지 않는다. */
	private static final boolean EXCLUDE_WITHDRAWN_AUTHORS = true;
	private final RecipeRewardPort rewards;
	private final GetUserUseCase users;
	private final EventTracker events;
	private final Clock clock;

	@Override
	@Transactional
	public boolean grant(Long userId, Long sharedRecipeId, LocalDate weekStart, int rank) {
		int amount = switch (rank) {
			case 1 -> 10;
			case 2 -> 6;
			case 3 -> 3;
			default -> throw new IllegalArgumentException("보상 순위는 1~3위입니다.");
		};
		// 제안: 탈퇴자만 제외한다. 정지 상태를 탈퇴와 동일 취급하지 않는다.
		var author = users.getUser(userId);
		if (author.isEmpty() || (EXCLUDE_WITHDRAWN_AUTHORS && author.get().deletedAt() != null)) {
			return false;
		}
		LocalDateTime now = LocalDateTime.now(clock.withZone(ZoneId.of("Asia/Seoul")));
		rewards.grant(userId, sharedRecipeId, weekStart, rank, amount, now, now.plusMonths(1));
		events.track(EventName.WEEKLY_REWARD_GRANTED, null,
				Map.of("reward_rank", rank, "reward_amount", amount), EventTrackCommand.ofUser(userId));
		return true;
	}
}
