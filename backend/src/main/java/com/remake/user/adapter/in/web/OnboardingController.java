package com.remake.user.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.user.application.port.in.CompleteOnboardingUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "온보딩")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

	private final CompleteOnboardingUseCase completeOnboardingUseCase;

	@Operation(summary = "온보딩 저장", description = "p1.2 완료 때 가구원 수와 제외 재료를 한 번에 저장한다. onboarding_completed 이벤트를 남긴다.")
	@PutMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void complete(@LoginUserId Long userId, @Valid @RequestBody OnboardingRequest request) {
		completeOnboardingUseCase.complete(request.toCommand(userId));
	}
}
