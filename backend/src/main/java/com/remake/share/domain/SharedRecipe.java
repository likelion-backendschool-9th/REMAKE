package com.remake.share.domain;

import java.time.LocalDateTime;

/** 작성자 정보는 원본 recipeId로만 연결하며 외부 응답에는 포함하지 않는다. */
public record SharedRecipe(Long id, Long recipeId, String title, String imageKey, Status status,
		LocalDateTime createdAt) {
	/** 기존 레시피 제목과 같은 상한. FE 입력도 같은 길이로 제한한다. */
	public static final int TITLE_MAX_LENGTH = 100;

	public enum Status { VISIBLE, HIDDEN }
}
