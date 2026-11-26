package com.remake.quota.application.port.in;

import java.time.LocalDate;

public interface GrantWeeklyRecipeRewardUseCase {
	/** 지급했으면 true. 제안: 탈퇴한 작성자에게 지급하지 않고 다음 순위로 넘기지도 않는다. */
	boolean grant(Long userId, Long sharedRecipeId, LocalDate weekStart, int rank);
}
