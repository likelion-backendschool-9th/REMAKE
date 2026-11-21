package com.remake.ingredient.adapter.out.persistence;

import java.util.Locale;

/** LIKE 검색 패턴 만들기. 검색어의 %, _, \는 글자 그대로 찾도록 이스케이프한다(JPQL에서 escape '\'와 함께 쓴다). */
final class LikePattern {

	private LikePattern() {
	}

	/** 소문자로 바꾼 "포함" 패턴. 검색어가 없으면 모든 이름과 맞는 % */
	static String contains(String keyword) {
		if (keyword == null || keyword.isBlank()) {
			return "%";
		}
		String escaped = keyword.strip().toLowerCase(Locale.ROOT)
				.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
