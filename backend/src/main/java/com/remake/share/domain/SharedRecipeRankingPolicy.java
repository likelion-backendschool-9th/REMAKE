package com.remake.share.domain;

import java.time.LocalDate;

public final class SharedRecipeRankingPolicy {
	public static final int TOP_LIMIT = 3;
	public static final int POPULAR_LIMIT = 100;
	public static final int MAX_PAGE_SIZE = 100;
	/** 제안: 본인 저장은 주간 집계에서 제외한다. #92 배치도 같은 기준을 사용해야 한다. */
	public static final boolean EXCLUDE_OWN_SAVES = true;
	/** 제안: 지난주와 같은 순위도 화면에서 “—”로 표시한다. */
	public static final boolean UNCHANGED_AS_DASH = true;

	private static final String[] ORDINALS = {"첫째", "둘째", "셋째", "넷째", "다섯째"};

	private SharedRecipeRankingPolicy() { }

	public static Integer rankChange(Integer rank, Integer previousRank) {
		if (rank == null || previousRank == null || (UNCHANGED_AS_DASH && rank.equals(previousRank))) {
			return null;
		}
		return previousRank - rank;
	}

	/** 주 시작 월요일이 속한 달에서 몇 번째 월요일인지 표시한다. 월·연 경계에도 한 주의 라벨은 같다. */
	public static String weekLabel(LocalDate monday) {
		return monday.getMonthValue() + "월 " + ORDINALS[(monday.getDayOfMonth() - 1) / 7] + " 주";
	}
}
