package com.remake.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

	@Test
	void Page를_공통_페이징_모양으로_바꾼다() {
		PageImpl<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 5);

		PageResponse<String> response = PageResponse.from(page);

		assertThat(response.content()).containsExactly("a", "b");
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(2);
		assertThat(response.totalElements()).isEqualTo(5);
		assertThat(response.hasNext()).isTrue();
	}

	@Test
	void 내용을_Response_DTO로_바꾸면서_페이징_정보는_유지한다() {
		PageImpl<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(1, 2), 4);

		PageResponse<String> response = PageResponse.from(page, number -> "#" + number);

		assertThat(response.content()).containsExactly("#1", "#2");
		assertThat(response.page()).isEqualTo(1);
		assertThat(response.hasNext()).isFalse();
	}
}
