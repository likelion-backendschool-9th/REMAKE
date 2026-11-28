package com.remake.subscription.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.subscription.application.port.in.CancelSubscriptionUseCase;
import com.remake.subscription.application.port.in.GetSubscriptionUseCase;
import com.remake.subscription.application.port.in.PrepareBillingAuthUseCase;
import com.remake.subscription.application.port.in.RefundSubscriptionUseCase;
import com.remake.subscription.application.port.in.StartSubscriptionUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "구독")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

	private final GetSubscriptionUseCase getSubscriptionUseCase;
	private final PrepareBillingAuthUseCase prepareBillingAuthUseCase;
	private final StartSubscriptionUseCase startSubscriptionUseCase;
	private final CancelSubscriptionUseCase cancelSubscriptionUseCase;
	private final RefundSubscriptionUseCase refundSubscriptionUseCase;

	@Operation(summary = "내 구독 상태·결제일 조회")
	@GetMapping
	public SubscriptionResponse get(@LoginUserId Long userId) {
		return SubscriptionResponse.from(getSubscriptionUseCase.get(userId));
	}

	@Operation(summary = "토스 카드 등록 창 초기값 조회")
	@PostMapping("/billing-auth")
	public BillingAuthResponse billingAuth(@LoginUserId Long userId) {
		return BillingAuthResponse.from(prepareBillingAuthUseCase.prepare(userId));
	}

	@Operation(summary = "구독 첫 결제·시작")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SubscriptionResponse start(@LoginUserId Long userId, @Valid @RequestBody StartSubscriptionRequest body) {
		return SubscriptionResponse.from(startSubscriptionUseCase.start(body.toCommand(userId)));
	}

	@Operation(summary = "구독 해지 예약")
	@PostMapping("/cancel")
	public SubscriptionResponse cancel(@LoginUserId Long userId) {
		return SubscriptionResponse.from(cancelSubscriptionUseCase.cancel(userId));
	}

	@Operation(summary = "7일 안 전액 환불")
	@PostMapping("/refund")
	public SubscriptionResponse refund(@LoginUserId Long userId) {
		return SubscriptionResponse.from(refundSubscriptionUseCase.refund(userId));
	}
}
