package com.remake.cart.domain;

public enum CartSource {
	OWN_RECIPE("own_recipe"), SHARED_RECIPE("shared_recipe"), MEAL_PLAN("meal_plan");

	private final String eventValue;
	CartSource(String eventValue) { this.eventValue = eventValue; }
	public String eventValue() { return eventValue; }
}
