package com.remake.subscription.adapter.in.web;

import com.remake.subscription.application.port.in.StartSubscriptionUseCase;

import jakarta.validation.constraints.NotBlank;

record StartSubscriptionRequest(
		@NotBlank String authKey,
		@NotBlank String customerKey) {

	StartSubscriptionUseCase.Command toCommand(Long userId) {
		return new StartSubscriptionUseCase.Command(userId, authKey, customerKey);
	}
}
