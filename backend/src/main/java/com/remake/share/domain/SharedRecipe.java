package com.remake.share.domain;

import java.time.LocalDateTime;

/** 작성자 정보는 원본 recipeId로만 연결하며 외부 응답에는 포함하지 않는다. */
public record SharedRecipe(Long id, Long recipeId, String title, String imageKey, Status status,
		LocalDateTime createdAt) {
	public enum Status { VISIBLE, HIDDEN }
}
