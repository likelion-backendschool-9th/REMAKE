package com.remake.event.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 이벤트 문자열 속성의 허용 값. 정의서·API/ERD에 나온 enum은 목록으로 검사하고,
 * 그 외 코드형 문자열은 토큰 형식만 허용해 이메일·자유 문장을 막는다.
 */
final class EventPropertyValues {

	private static final Pattern SAFE_TOKEN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,63}$");
	private static final Pattern CODE_TOKEN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

	private static final Map<String, Set<String>> ENUM_VALUES = Map.ofEntries(
			Map.entry("entry_source", Set.of("icon", "splash", "push", "browser", "share", "deep_link", "notification")),
			Map.entry("provider", Set.of("kakao", "naver", "google")),
			Map.entry("permission_result", Set.of("granted", "denied", "dismissed")),
			Map.entry("limit_feature", Set.of("RECIPE", "MEAL_PLAN")),
			Map.entry("onboarding_step", Set.of("household", "excluded", "notification", "done")),
			Map.entry("setting_key", Set.of("recipeSupplementEnabled", "notificationEnabled")),
			Map.entry("filter_type", Set.of("storage", "category", "status", "q")),
			Map.entry("add_source", Set.of("MANUAL", "RECEIPT", "CATALOG")),
			Map.entry("storage", Set.of("FRIDGE", "FREEZER", "PANTRY")),
			Map.entry("edit_field", Set.of(
					"name", "category", "storage", "expiration_date", "amount", "unit", "price", "item_name")),
			Map.entry("use_via", Set.of("manual", "cook")),
			Map.entry("expiry_status", Set.of("danger", "soon", "ok")),
			Map.entry("scan_method", Set.of("CAMERA", "GALLERY")),
			Map.entry("recipe_mode", Set.of("QUICK", "CUSTOM")),
			Map.entry("detail_origin", Set.of("LIST", "MEAL_PLAN", "SHARED", "GENERATE", "SAVED")),
			Map.entry("save_source", Set.of("RECIPE", "SHARED_RECIPE")),
			Map.entry("feedback_value", Set.of("LIKE", "DISLIKE")),
			Map.entry("meal_purpose", Set.of("BALANCED", "DIET", "HIGH_PROTEIN", "SIMPLE", "SAVING")),
			Map.entry("meal_type", Set.of("BREAKFAST", "LUNCH", "DINNER")),
			Map.entry("date_relation", Set.of("TODAY", "PAST", "FUTURE")),
			Map.entry("slot_source", Set.of("AI", "MANUAL")),
			Map.entry("cart_source", Set.of("MEAL_PLAN", "RECIPE_SAVE", "SHARED_RECIPE_SAVE")),
			Map.entry("notification_type", Set.of(
					"expiry_soon", "expired", "meal_plan_completed", "meal_plan_failed")),
			Map.entry("ranking_view", Set.of("WEEKLY", "ALL")),
			Map.entry("view_from", Set.of("RANKING", "SHARE_LINK", "SAVED")),
			Map.entry("rank_bucket", Set.of("TOP10", "TOP30", "OTHER")),
			Map.entry("report_reason", Set.of("PHOTO", "OFFENSIVE", "DANGEROUS", "OTHER")));

	private static final Set<String> CODE_PROPERTIES = Set.of("error_code", "category_code");
	private static final Set<String> SCREEN_PROPERTIES = Set.of("screen_name", "previous_screen");

	private EventPropertyValues() {
	}

	static boolean isAllowedString(String property, String value) {
		if (value == null || value.isBlank()) {
			return false;
		}
		Set<String> allowed = ENUM_VALUES.get(property);
		if (allowed != null) {
			return allowed.contains(value);
		}
		if (CODE_PROPERTIES.contains(property)) {
			return CODE_TOKEN.matcher(value).matches();
		}
		if (SCREEN_PROPERTIES.contains(property)) {
			return Screen.fromCode(value) != Screen.UNKNOWN
					|| SAFE_TOKEN.matcher(value.toLowerCase(Locale.ROOT)).matches();
		}
		if ("filter_value".equals(property)) {
			return SAFE_TOKEN.matcher(value).matches() || CODE_TOKEN.matcher(value).matches();
		}
		return SAFE_TOKEN.matcher(value).matches();
	}
}
