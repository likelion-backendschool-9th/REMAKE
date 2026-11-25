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
				Map.of("cart_source", "meal_plan", "item_count", 3))).isPresent();
		assertThat(EventPropertyFilter.filter(EventName.CART_ITEM_ADDED,
				Map.of("cart_source", "FREE_TEXT", "item_count", 3))).isEmpty();
	}
	@Test
	void 레시피저장과_취소는_명세의_소문자출처만_허용한다() {
		for (EventName event : new EventName[]{EventName.RECIPE_SAVED, EventName.RECIPE_UNSAVED}) {
			for (String source : new String[]{"own", "shared"}) {
				assertThat(EventPropertyFilter.filter(event, Map.of("save_source", source)))
						.contains(Map.of("save_source", source));
			}
			for (String source : new String[]{"RECIPE", "SHARED_RECIPE", "unknown"}) {
				assertThat(EventPropertyFilter.filter(event, Map.of("save_source", source))).isEmpty();
			}
		}
		assertThat(EventName.fromWireName("shared_recipe_saved")).isEmpty();
	}

}
