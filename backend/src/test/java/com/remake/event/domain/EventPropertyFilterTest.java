package com.remake.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class EventPropertyFilterTest {

	@Test
	void 허용_속성만_남기고_금지_키와_미허용_키는_버린다() {
		Map<String, Object> raw = new LinkedHashMap<>();
		raw.put("provider", "kakao");
		raw.put("is_new_user", true);
		raw.put("email", "a@b.com");
		raw.put("extra", 1);

		Map<String, Object> filtered = EventPropertyFilter.filter(EventName.LOGIN_SUCCEEDED, raw).orElseThrow();

		assertThat(filtered).containsExactly(
				Map.entry("provider", "kakao"),
				Map.entry("is_new_user", true));
	}

	@Test
	void 속성이_25개를_넘으면_거절한다() {
		Map<String, Object> raw = new LinkedHashMap<>();
		for (int i = 0; i < 26; i++) {
			raw.put("provider", "x"); // size counts keys; use unique keys
		}
		raw.clear();
		for (int i = 0; i < 26; i++) {
			raw.put("k" + i, i);
		}

		assertThat(EventPropertyFilter.filter(EventName.LOGIN_SUCCEEDED, raw)).isEmpty();
	}

	@Test
	void 중첩_값은_거절한다() {
		assertThat(EventPropertyFilter.filter(EventName.LOGIN_SUCCEEDED,
				Map.of("provider", Map.of("nested", 1)))).isEmpty();
		assertThat(EventPropertyFilter.filter(EventName.LOGIN_SUCCEEDED,
				Map.of("provider", List.of("a")))).isEmpty();
	}

	@Test
	void 공통만_있는_이벤트는_속성을_전부_버린다() {
		Map<String, Object> filtered = EventPropertyFilter.filter(EventName.LOGOUT_CONFIRMED, Map.of("foo", 1))
				.orElseThrow();
		assertThat(filtered).isEmpty();
	}
}
