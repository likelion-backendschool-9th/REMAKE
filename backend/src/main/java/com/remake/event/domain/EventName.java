package com.remake.event.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 이벤트 이름·허용 속성·발송 주체. docs/agent/spec/events.md 와 1:1.
 * 문자열로 이벤트 이름을 쓰지 말고 이 enum만 쓴다.
 */
@Getter
@RequiredArgsConstructor
public enum EventName {

	APP_OPENED("app_opened", EventSource.CLIENT, Set.of("entry_source", "is_standalone")), // E01
	SCREEN_VIEWED("screen_viewed", EventSource.CLIENT, Set.of("screen_name", "previous_screen")), // E02
	ERROR_SHOWN("error_shown", EventSource.CLIENT, Set.of("error_code", "http_status", "screen_name")), // E03
	PWA_INSTALL_PROMPTED("pwa_install_prompted", EventSource.CLIENT, Set.of()), // E04
	PWA_INSTALLED("pwa_installed", EventSource.CLIENT, Set.of()), // E05
	PUSH_PERMISSION_RESULT("push_permission_result", EventSource.CLIENT, Set.of("permission_result")), // E06
	QUOTA_EXCEEDED("quota_exceeded", EventSource.SERVER, Set.of("limit_feature", "limit_total", "used_count")), // E07
	LOGIN_STARTED("login_started", EventSource.CLIENT, Set.of("provider", "is_recent_provider")), // E08
	LOGIN_SUCCEEDED("login_succeeded", EventSource.SERVER, Set.of("provider", "is_new_user")), // E09
	LOGIN_FAILED("login_failed", EventSource.SERVER, Set.of("provider", "error_code")), // E10
	SIGNUP_COMPLETED("signup_completed", EventSource.SERVER, Set.of("provider")), // E11
	ONBOARDING_STEP_COMPLETED("onboarding_step_completed", EventSource.CLIENT, Set.of("onboarding_step")), // E12
	ONBOARDING_COMPLETED("onboarding_completed", EventSource.SERVER, Set.of("excluded_count")), // E13
	SETTING_CHANGED("setting_changed", EventSource.CLIENT, Set.of("setting_key", "new_value_bool")), // E14
	EXCLUDED_INGREDIENTS_UPDATED("excluded_ingredients_updated", EventSource.CLIENT, Set.of("excluded_count")), // E15
	LOGOUT_CONFIRMED("logout_confirmed", EventSource.CLIENT, Set.of()), // E16
	ACCOUNT_DELETION_REQUESTED("account_deletion_requested", EventSource.SERVER, Set.of("is_resend")), // E17
	ACCOUNT_DELETED("account_deleted", EventSource.SERVER, Set.of("tenure_days")), // E18
	FRIDGE_LIST_VIEWED("fridge_list_viewed", EventSource.CLIENT, Set.of("total_count", "danger_count", "soon_count", "ok_count")), // E19
	FRIDGE_FILTER_APPLIED("fridge_filter_applied", EventSource.CLIENT, Set.of("filter_type", "filter_value")), // E20
	INGREDIENT_ADD_STARTED("ingredient_add_started", EventSource.CLIENT, Set.of("add_source")), // E21
	INGREDIENT_ADDED("ingredient_added", EventSource.SERVER, Set.of("add_source", "category_code", "storage", "has_price", "is_expiration_estimated", "duration_sec")), // E22
	INGREDIENT_EDITED("ingredient_edited", EventSource.CLIENT, Set.of("edit_field")), // E23
	INGREDIENT_DELETED("ingredient_deleted", EventSource.CLIENT, Set.of("category_code")), // E24
	INGREDIENT_USED("ingredient_used", EventSource.SERVER, Set.of("use_via", "category_code", "expiry_status", "used_ratio")), // E25
	INGREDIENT_DISCARDED("ingredient_discarded", EventSource.SERVER, Set.of("category_code", "expiry_status", "used_ratio", "has_price", "cost_amount")), // E26
	INGREDIENT_EXPIRED("ingredient_expired", EventSource.SERVER, Set.of("category_code", "has_price")), // E27
	RECEIPT_SCAN_STARTED("receipt_scan_started", EventSource.CLIENT, Set.of("scan_method")), // E28
	CAMERA_UNAVAILABLE("camera_unavailable", EventSource.CLIENT, Set.of()), // E29
	RECEIPT_SCAN_SUCCEEDED("receipt_scan_succeeded", EventSource.SERVER, Set.of("item_count", "food_item_count", "duration_ms", "duplicate_suspected")), // E30
	RECEIPT_SCAN_FAILED("receipt_scan_failed", EventSource.SERVER, Set.of("error_code", "duration_ms")), // E31
	RECEIPT_ITEM_EDITED("receipt_item_edited", EventSource.CLIENT, Set.of("item_index", "edit_field")), // E32
	RECEIPT_ITEM_ADDED_MANUALLY("receipt_item_added_manually", EventSource.CLIENT, Set.of()), // E33
	RECEIPT_SAVED("receipt_saved", EventSource.SERVER, Set.of("receipt_id", "registered_count", "ocr_item_count", "edited_item_count", "deleted_item_count", "added_item_count", "duration_sec", "food_total_amount")), // E34
	RECEIPT_RESULT_ABANDONED("receipt_result_abandoned", EventSource.CLIENT, Set.of("ocr_item_count", "edited_item_count", "deleted_item_count", "added_item_count", "duration_sec")), // E76
	RECEIPT_LIST_VIEWED("receipt_list_viewed", EventSource.CLIENT, Set.of("month_offset", "has_comparison")), // E35
	RECEIPT_DETAIL_VIEWED("receipt_detail_viewed", EventSource.CLIENT, Set.of()), // E36
	RECEIPT_DELETED("receipt_deleted", EventSource.CLIENT, Set.of()), // E37
	CONSUMPTION_PATTERN_VIEWED("consumption_pattern_viewed", EventSource.CLIENT, Set.of("month_offset")), // E38
	RECIPE_GENERATE_STARTED("recipe_generate_started", EventSource.CLIENT, Set.of("recipe_mode", "servings", "option_count", "supplement_enabled")), // E39
	RECIPE_GENERATED("recipe_generated", EventSource.SERVER, Set.of("recipe_id", "recipe_mode", "duration_ms", "expiring_used_count", "shortage_count", "allergen_warning", "remaining_quota")), // E40
	RECIPE_GENERATE_FAILED("recipe_generate_failed", EventSource.SERVER, Set.of("recipe_mode", "error_code", "duration_ms")), // E41
	RECIPE_REGENERATED("recipe_regenerated", EventSource.CLIENT, Set.of("recipe_mode")), // E42
	RECIPE_DETAIL_VIEWED("recipe_detail_viewed", EventSource.CLIENT, Set.of("recipe_id", "detail_origin")), // E43
	RECIPE_SAVED("recipe_saved", EventSource.SERVER, Set.of("recipe_id", "save_source", "cart_added_count")), // E44
	RECIPE_UNSAVED("recipe_unsaved", EventSource.CLIENT, Set.of("save_source")), // E45
	RECIPE_SAVED_LIST_VIEWED("recipe_saved_list_viewed", EventSource.CLIENT, Set.of("saved_count")), // E46
	STEP_PLAY_STARTED("step_play_started", EventSource.CLIENT, Set.of("recipe_id")), // E47
	COOK_DEDUCT_VIEWED("cook_deduct_viewed", EventSource.CLIENT, Set.of("recipe_id", "deduct_item_count", "needs_check_count")), // E48
	COOK_COMPLETED("cook_completed", EventSource.SERVER, Set.of("recipe_id", "deducted_count", "adjusted_count", "servings")), // E49
	COOK_FEEDBACK_ANSWERED("cook_feedback_answered", EventSource.CLIENT, Set.of("feedback_value")), // E50
	MEAL_PLAN_SETUP_VIEWED("meal_plan_setup_viewed", EventSource.CLIENT, Set.of("remaining_quota")), // E51
	MEAL_PLAN_GENERATE_STARTED("meal_plan_generate_started", EventSource.CLIENT, Set.of("meal_purpose", "meal_type_count", "servings", "start_offset_days")), // E52
	MEAL_PLAN_GENERATED("meal_plan_generated", EventSource.SERVER, Set.of("meal_plan_id", "meal_purpose", "meal_type_count", "generated_days", "shortage_count", "duration_ms", "remaining_quota")), // E53
	MEAL_PLAN_GENERATE_FAILED("meal_plan_generate_failed", EventSource.SERVER, Set.of("error_code", "duration_ms")), // E54
	MEAL_PLAN_SHORTAGE_SHOWN("meal_plan_shortage_shown", EventSource.CLIENT, Set.of("shortage_count", "generated_days")), // E55
	MEAL_CALENDAR_VIEWED("meal_calendar_viewed", EventSource.CLIENT, Set.of("month_offset", "has_plan")), // E56
	MEAL_DAY_VIEWED("meal_day_viewed", EventSource.CLIENT, Set.of("date_relation")), // E57
	MEAL_SLOT_OPENED("meal_slot_opened", EventSource.CLIENT, Set.of("meal_type", "slot_source", "has_recipe")), // E58
	MEAL_SLOT_DETAIL_GENERATED("meal_slot_detail_generated", EventSource.SERVER, Set.of("recipe_id", "duration_ms")), // E59
	MEAL_SLOT_DETAIL_FAILED("meal_slot_detail_failed", EventSource.SERVER, Set.of("error_code", "duration_ms")), // E60
	MEAL_ADDED("meal_added", EventSource.SERVER, Set.of("meal_type", "date_relation", "recipe_id")), // E61
	CART_ITEM_ADDED("cart_item_added", EventSource.SERVER, Set.of("cart_source", "item_count")), // E62
	CART_VIEWED("cart_viewed", EventSource.CLIENT, Set.of("group_count", "item_count")), // E63
	CART_AMOUNT_CHANGED("cart_amount_changed", EventSource.CLIENT, Set.of()), // E64
	CART_ITEMS_DELETED("cart_items_deleted", EventSource.CLIENT, Set.of("deleted_count", "was_select_all")), // E65
	NOTIFICATION_SENT("notification_sent", EventSource.SERVER, Set.of("notification_type", "batch_count")), // E66
	NOTIFICATION_BANNER_CLICKED("notification_banner_clicked", EventSource.CLIENT, Set.of("unread_count")), // E67
	NOTIFICATION_LIST_VIEWED("notification_list_viewed", EventSource.CLIENT, Set.of("unread_count")), // E68
	RANKING_VIEWED("ranking_viewed", EventSource.CLIENT, Set.of("ranking_view", "query_used")), // E69
	SHARED_RECIPE_VIEWED("shared_recipe_viewed", EventSource.CLIENT, Set.of("shared_recipe_id", "view_from", "rank_bucket")), // E70
	SHARED_WRITE_STARTED("shared_write_started", EventSource.CLIENT, Set.of("recipe_id")), // E71
	SHARED_RECIPE_PUBLISHED("shared_recipe_published", EventSource.SERVER, Set.of("shared_recipe_id", "photo_count")), // E72
	SHARED_RECIPE_SAVED("shared_recipe_saved", EventSource.SERVER, Set.of("shared_recipe_id", "cart_added_count")), // E73
	SHARED_RECIPE_REPORTED("shared_recipe_reported", EventSource.SERVER, Set.of("shared_recipe_id", "report_reason")), // E74
	WEEKLY_REWARD_GRANTED("weekly_reward_granted", EventSource.SERVER, Set.of("reward_rank", "reward_amount")); // E75

	private final String wireName;
	private final EventSource source;
	private final Set<String> allowedProperties;

	private static final Map<String, EventName> BY_WIRE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(EventName::getWireName, Function.identity()));

	public static Optional<EventName> fromWireName(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(BY_WIRE.get(name.trim().toLowerCase(Locale.ROOT)));
	}
}
