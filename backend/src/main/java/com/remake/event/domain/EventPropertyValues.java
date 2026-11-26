package com.remake.event.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 이벤트 문자열 속성의 허용 값. 정의서·API/ERD에 나온 enum은 목록으로 검사하고,
 * 그 외 코드형 문자열은 토큰 형식만 허용해 이메일·자유 문장을 막는다.
 * 코드 값은 소문자다. storage·filter_value(상태·보관 위치)만 정의서대로 대문자를 유지한다(#172).
 */
final class EventPropertyValues {

	private static final Pattern SAFE_TOKEN = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,63}$");
	private static final Pattern CODE_TOKEN = Pattern.compile("^[A-Z][A-Z0-9_]{0,63}$");

	private static final Map<String, Set<String>> ENUM_VALUES = Map.ofEntries(
			Map.entry("entry_source", Set.of("direct", "shortcut", "link")),
			Map.entry("provider", Set.of("kakao", "naver", "google")),
			Map.entry("deletion_method", Set.of("email_code", "reauth")),
			Map.entry("end_reason", Set.of("canceled", "account_deleted")),
			Map.entry("limit_feature", Set.of("recipe", "meal_plan")),
			Map.entry("onboarding_step", Set.of("household", "allergy")),
			Map.entry("setting_key", Set.of("household_size", "recipe_supplement", "notification")),
			Map.entry("filter_type", Set.of("status", "location", "category")),
			Map.entry("add_source", Set.of("manual", "receipt", "cart")),
			Map.entry("storage", Set.of("FRIDGE", "FREEZER", "PANTRY")),
			Map.entry("edit_field", Set.of("name", "category", "storage", "amount", "expiration", "price")),
			Map.entry("use_via", Set.of("manual", "cook")),
			Map.entry("expiry_status", Set.of("danger", "soon", "ok")),
			Map.entry("scan_method", Set.of("camera", "upload")),
			Map.entry("recipe_mode", Set.of("quick", "custom")),
			Map.entry("detail_origin", Set.of("saved", "meal_plan")),
			Map.entry("save_source", Set.of("own", "shared")),
			Map.entry("meal_purpose", Set.of("balanced", "diet", "high_protein", "simple", "saving")),
			Map.entry("meal_type", Set.of("breakfast", "lunch", "dinner")),
			Map.entry("date_relation", Set.of("past", "today", "future")),
			Map.entry("cart_source", Set.of("meal_plan", "own_recipe", "shared_recipe")),
			Map.entry("notification_type", Set.of(
					"expiry_soon", "expired", "meal_plan_completed", "meal_plan_failed")),
			Map.entry("ranking_view", Set.of("top3", "full")),
			Map.entry("view_from", Set.of("top3", "ranking", "search")),
			Map.entry("rank_bucket", Set.of("1-3", "4-10", "11-100", "none")),
			Map.entry("overlap_choice", Set.of("overwrite", "cancel")),
			Map.entry("ranking_sort", Set.of("popular", "latest")),
			Map.entry("list_filter", Set.of("all", "favorite", "available")),
			Map.entry("list_sort", Set.of("saved_desc", "saved_asc")),
			Map.entry("report_reason", Set.of("offensive_image", "ad_title", "dangerous_recipe", "other")));

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
