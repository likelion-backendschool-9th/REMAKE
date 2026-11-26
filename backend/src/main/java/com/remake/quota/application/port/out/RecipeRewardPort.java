package com.remake.quota.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface RecipeRewardPort {
	void grant(Long userId, Long sharedRecipeId, LocalDate weekStart, int rank, int amount,
			LocalDateTime now, LocalDateTime expiresAt);
	int remaining(Long userId, LocalDateTime now);
	/** 회원별 생성 차감 잠금 안에서 호출한다. 만료가 가까운 지급 건부터 한 번 차감한다. */
	Optional<Long> consume(Long userId, LocalDateTime now);
}
