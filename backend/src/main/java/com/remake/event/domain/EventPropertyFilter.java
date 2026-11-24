package com.remake.event.domain;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 이벤트 속성 필터. 허용 목록 밖·금지 키는 버리고, null·25개 초과·중첩·비허용 타입·허용되지 않은
 * 문자열 값은 이벤트 전체를 거절한다. 개인정보·자유 입력(이메일, 이름, 재료명 등)이 실수로
 * 들어오지 않게 막는다.
 */
public final class EventPropertyFilter {

	public static final int MAX_PROPERTIES = 25;

	private static final Set<String> FORBIDDEN_KEYS = Set.of(
			"email",
			"name",
			"user_name",
			"username",
			"display_name",
			"provider_user_id",
			"social_id",
			"ingredient_name",
			"recipe_name",
			"recipe_content",
			"allergy",
			"allergies",
			"excluded_ingredient_name",
			"password",
			"token",
			"access_token",
			"refresh_token");

	private EventPropertyFilter() {
	}

	/**
	 * @return 통과한 속성. 거절이면 empty (이벤트 자체를 버림)
	 */
	public static Optional<Map<String, Object>> filter(EventName eventName, Map<String, ?> raw) {
		if (raw == null || raw.isEmpty()) {
			return Optional.of(Map.of());
		}
		if (raw.size() > MAX_PROPERTIES) {
			return Optional.empty();
		}
		Map<String, Object> cleaned = new LinkedHashMap<>();
		for (Map.Entry<String, ?> entry : raw.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if (key == null || key.isBlank()) {
				continue;
			}
			String normalized = key.trim();
			if (isForbidden(normalized)) {
				continue;
			}
			if (!eventName.getAllowedProperties().contains(normalized)) {
				continue;
			}
			if (eventName == EventName.RECEIPT_ITEM_EDITED && normalized.equals("edit_field")
					&& "expiration_date".equals(value)) {
				value = "expiration";
			}
			if (!isAllowedValue(normalized, value)) {
				return Optional.empty();
			}
			cleaned.put(normalized, value);
		}
		if (cleaned.size() > MAX_PROPERTIES) {
			return Optional.empty();
		}
		return Optional.of(Map.copyOf(cleaned));
	}

	private static boolean isForbidden(String key) {
		String lower = key.toLowerCase(Locale.ROOT);
		if (FORBIDDEN_KEYS.contains(lower)) {
			return true;
		}
		return lower.contains("email") || lower.endsWith("_token") || lower.contains("password");
	}

	private static boolean isAllowedValue(String property, Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Map<?, ?> || value instanceof Collection<?> || value.getClass().isArray()) {
			return false;
		}
		if (value instanceof Boolean || value instanceof Number) {
			return true;
		}
		if (value instanceof String stringValue) {
			return EventPropertyValues.isAllowedString(property, stringValue);
		}
		return false;
	}
}
