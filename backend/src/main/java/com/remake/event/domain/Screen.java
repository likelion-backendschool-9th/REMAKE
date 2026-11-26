package com.remake.event.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 화면 코드. 이벤트 정의서 41개·프론트 lib/events/screens.ts와 맞춘다.
 * 서버 EventTracker는 이 enum을 쓰고, 클라이언트 수집 API는 문자열 코드를 그대로 저장한다.
 * UNKNOWN은 알 수 없는 코드용이며, 스케줄 작업처럼 화면이 없으면 track에 null을 넘긴다.
 */
@Getter
@RequiredArgsConstructor
public enum Screen {

	SPLASH("splash"),
	LOGIN("login"),
	ONBOARDING_HOUSEHOLD("onboarding_household"),
	ONBOARDING_ALLERGY("onboarding_allergy"),
	NOTIFICATION_LIST("notification_list"),
	SETTING_MAIN("setting_main"),
	LOGOUT_MODAL("logout_modal"),
	SETTING_HOUSEHOLD("setting_household"),
	SETTING_ALLERGY("setting_allergy"),
	ACCOUNT_DELETE_MODAL("account_delete_modal"),
	ACCOUNT_DELETE_CODE("account_delete_code"),
	SUBSCRIPTION_MANAGE("subscription_manage"),
	FRIDGE_LIST("fridge_list"),
	INGREDIENT_CONSUME("ingredient_consume"),
	INGREDIENT_EDIT("ingredient_edit"),
	INGREDIENT_ADD("ingredient_add"),
	RECEIPT_LIST("receipt_list"),
	RECEIPT_SCAN("receipt_scan"),
	RECEIPT_LOADING("receipt_loading"),
	RECEIPT_SCAN_ERROR("receipt_scan_error"),
	RECEIPT_RESULT("receipt_result"),
	RECEIPT_ITEM_EDIT("receipt_item_edit"),
	RECEIPT_ITEM_ADD("receipt_item_add"),
	RECEIPT_DETAIL("receipt_detail"),
	RECEIPT_PATTERN("receipt_pattern"),
	RECIPE_HOME("recipe_home"),
	RECIPE_CREATE("recipe_create"),
	RECIPE_CUSTOM("recipe_custom"),
	RECIPE_LOADING("recipe_loading"),
	RECIPE_GENERATE_ERROR("recipe_generate_error"),
	RECIPE_RESULT("recipe_result"),
	RECIPE_DETAIL("recipe_detail"),
	COOK_DEDUCT("cook_deduct"),
	SHARED_WRITE("shared_write"),
	RECIPE_RANKING("recipe_ranking"),
	SHARED_DETAIL("shared_detail"),
	SHARED_REPORT("shared_report"),
	MEAL_PLAN_CALENDAR("meal_plan_calendar"),
	MEAL_PLAN_SETUP("meal_plan_setup"),
	MEAL_PLAN_OVERLAP_MODAL("meal_plan_overlap_modal"),
	CART_LIST("cart_list"),
	UNKNOWN("unknown");

	private final String code;

	private static final Map<String, Screen> BY_CODE = Arrays.stream(values())
			.collect(Collectors.toUnmodifiableMap(s -> s.code, Function.identity()));

	public static Screen fromCode(String code) {
		if (code == null || code.isBlank()) {
			return UNKNOWN;
		}
		return BY_CODE.getOrDefault(code.trim().toLowerCase(Locale.ROOT), UNKNOWN);
	}
}
