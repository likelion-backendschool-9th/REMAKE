package com.remake.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
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

		assertThat(filtered).containsOnly(
				Map.entry("provider", "kakao"),
				Map.entry("is_new_user", true));
	}

	@Test
	void 속성이_25개를_넘으면_거절한다() {
		Map<String, Object> raw = new LinkedHashMap<>();
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

	@Test
	void null_속성_값은_이벤트를_거절한다() {
		Map<String, Object> raw = new HashMap<>();
		raw.put("entry_source", null);
		raw.put("is_standalone", true);

		assertThat(EventPropertyFilter.filter(EventName.APP_OPENED, raw)).isEmpty();
	}

	@Test
	void 허용된_문자열_속성에_자유_입력이_오면_거절한다() {
		assertThat(EventPropertyFilter.filter(EventName.APP_OPENED,
				Map.of("entry_source", "user@example.com", "is_standalone", true))).isEmpty();
		assertThat(EventPropertyFilter.filter(EventName.APP_OPENED,
				Map.of("entry_source", "직접 입력한 문장", "is_standalone", true))).isEmpty();
	}

	@Test
	void 정해진_문자열_허용값만_통과한다() {
		assertThat(EventPropertyFilter.filter(EventName.APP_OPENED,
				Map.of("entry_source", "icon", "is_standalone", true))).isPresent();
		assertThat(EventPropertyFilter.filter(EventName.CART_ITEM_ADDED,
				Map.of("cart_source", "MEAL_PLAN", "item_count", 3))).isPresent();
		assertThat(EventPropertyFilter.filter(EventName.CART_ITEM_ADDED,
				Map.of("cart_source", "FREE_TEXT", "item_count", 3))).isEmpty();
	}
}
