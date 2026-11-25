package com.remake.user.adapter.in.web;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.user.application.port.in.GetSubscriptionUseCase;

@JsonInclude(JsonInclude.Include.NON_NULL)
record SubscriptionResponse(
		String status,
		long price,
		LocalDate nextBillingDate,
		LocalDate currentPeriodEnd) {

	static SubscriptionResponse from(GetSubscriptionUseCase.SubscriptionView view) {
		return new SubscriptionResponse(view.status().name(), view.price(), view.nextBillingDate(),
				view.currentPeriodEnd());
	}
}
