package com.remake.user.adapter.in.web;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.user.application.port.in.GetSubscriptionUseCase;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SubscriptionResponse(
		@Schema(allowableValues = {"NONE", "ACTIVE", "CANCELED"}) String status,
		long price,
		@Schema(nullable = true) LocalDate nextBillingDate,
		@Schema(nullable = true) LocalDate currentPeriodEnd) {

	static SubscriptionResponse from(GetSubscriptionUseCase.SubscriptionView view) {
		return new SubscriptionResponse(view.status().name(), view.price(), view.nextBillingDate(),
				view.currentPeriodEnd());
	}
}
