package com.remake.share.domain;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

/** 작성자 정보는 원본 recipeId로만 연결하며 외부 응답에는 포함하지 않는다. */
public record SharedRecipe(Long id, Long recipeId, String title, String imageKey, Status status,
		LocalDateTime createdAt) {
	/** 기존 레시피 제목과 같은 상한. FE 입력도 같은 길이로 제한한다. */
	public static final int TITLE_MAX_LENGTH = 100;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	public enum Status { VISIBLE, HIDDEN }

	/** 같은 원본은 이 시각부터 1주(KST 월요일 0시 시작) 안에 한 번만 공유한다. 게시(51)와 상세의 shareable(30)이 같이 쓴다. */
	public static LocalDateTime weekStart(Clock clock) {
		return LocalDate.now(clock.withZone(KST))
				.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
	}
}
