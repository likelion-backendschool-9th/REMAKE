package com.remake.quota.domain;

import java.time.LocalDateTime;

/**
 * 오늘의 레시피 생성 횟수.
 *
 * @param limit          요금제의 하루 기본 횟수
 * @param used           오늘 성공한 생성 수(기본·보상 합계)
 * @param bonusRemaining 아직 쓸 수 있는 보상 횟수(만료 전). 보상 테이블이 생기기 전까지 0
 * @param resetsAt       기본 횟수가 다시 채워지는 시각(내일 00:00, 한국 시간)
 */
public record RecipeQuota(int limit, int used, int bonusRemaining, LocalDateTime resetsAt) {

	/** 남은 기본 횟수 + 보상 횟수. 기본 횟수를 다 쓴 뒤에 보상을 쓴다. */
	public int remaining() {
		return Math.max(limit - used, 0) + bonusRemaining;
	}

	public boolean exhausted() {
		return remaining() <= 0;
	}
}
