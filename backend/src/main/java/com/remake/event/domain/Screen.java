package com.remake.event.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 화면 코드. 이벤트 정의서·프론트 lib/events/screens.ts와 맞춘다.
 * 서버 EventTracker는 이 enum을 쓰고, 클라이언트 수집 API는 문자열 코드를 그대로 저장한다.
 */
@Getter
@RequiredArgsConstructor
public enum Screen {

	LOGIN("login"),
	ONBOARDING("onboarding"),
	SETTINGS("settings"),
	SUBSCRIPTION_MANAGE("subscription_manage"),
	FRIDGE_LIST("fridge_list"),
	INGREDIENT_ADD("ingredient_add"),
	RECEIPT_SCAN("receipt_scan"),
	RECEIPT_RESULT("receipt_result"),
	RECEIPT_LIST("receipt_list"),
	RECEIPT_DETAIL("receipt_detail"),
	CONSUMPTION_PATTERN("consumption_pattern"),
	RECIPE_CREATE("recipe_create"),
	RECIPE_CUSTOM("recipe_custom"),
	RECIPE_DETAIL("recipe_detail"),
	RECIPE_SAVED_LIST("recipe_saved_list"),
	COOK_DEDUCT("cook_deduct"),
	MEAL_PLAN_SETUP("meal_plan_setup"),
	MEAL_PLAN_CALENDAR("meal_plan_calendar"),
	MEAL_DAY("meal_day"),
	MEAL_SLOT("meal_slot"),
	CART("cart"),
	CART_LIST("cart_list"),
	RANKING("ranking"),
	SHARED_RECIPE("shared_recipe"),
	SHARED_REPORT("shared_report"),
	SHARED_WRITE("shared_write"),
	NOTIFICATION_LIST("notification_list"),
	ADMIN("admin"),
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
