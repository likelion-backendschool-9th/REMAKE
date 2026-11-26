package com.remake.user.adapter.in.web;

import com.remake.user.application.port.in.PrepareBillingAuthUseCase;

record BillingAuthResponse(String customerKey, String orderName, long amount) {

	static BillingAuthResponse from(PrepareBillingAuthUseCase.BillingAuthView view) {
		return new BillingAuthResponse(view.customerKey(), view.orderName(), view.amount());
	}
}
