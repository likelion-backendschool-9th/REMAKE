package com.remake.global.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 목록 API의 공통 페이징 응답. 모양: { content, page, size, totalElements, hasNext }
 * 요청은 ?page=0&size=20 (page는 0부터).
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, boolean hasNext) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.hasNext());
	}

	// 엔티티나 도메인 객체의 Page를 Response DTO로 바꿀 때 쓴다. 예: PageResponse.from(page, IngredientResponse::from)
	public static <S, T> PageResponse<T> from(Page<S> page, Function<? super S, ? extends T> mapper) {
		return from(page.map(mapper));
	}
}
