package com.remake.quota.domain;

import java.time.LocalDateTime;

/**
 * 오늘의 레시피 생성 횟수.
 *
 * @param limit          요금제의 하루 기본 횟수
 * @param used           오늘 성공한 생성 수(기본·보상 합계)
 * @param baseUsed       오늘 기본 횟수로 성공한 생성 수(보상 횟수로 쓴 건 제외). 남은 기본 횟수는 이 값으로 계산한다.
 * @param bonusRemaining 아직 쓸 수 있는 보상 횟수(만료 전).
 * @param resetsAt       기본 횟수가 다시 채워지는 시각(내일 00:00, 한국 시간)
 */
public record RecipeQuota(int limit, int used, int baseUsed, int bonusRemaining, LocalDateTime resetsAt) {

	/**
	 * 남은 기본 횟수 + 보상 횟수. 기본 횟수를 다 쓴 뒤에 보상을 쓴다.
	 * 보상으로 쓴 횟수를 기본 한도에서 다시 빼지 않도록 used가 아니라 baseUsed로 계산한다
	 * (같은 날 BASIC → PREMIUM으로 바뀌어도 기본 횟수가 이중 차감되지 않음).
	 */
	public int remaining() {
		return Math.max(limit - baseUsed, 0) + bonusRemaining;
	}

	public boolean exhausted() {
		return remaining() <= 0;
	}
}
