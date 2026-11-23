package com.remake.quota.adapter.in.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.quota.application.port.in.GetUsageQuotasUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 남은 생성 횟수(API 27). */
@Tag(name = "생성 횟수")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequiredArgsConstructor
public class UsageQuotaController {

	private final GetUsageQuotasUseCase getUsageQuotasUseCase;

	@Operation(summary = "남은 생성 횟수", description = "레시피는 오늘(한국 시간 00:00부터), 식단표는 이번 달(1일부터) 기준입니다. 성공한 생성만 셉니다.")
	@GetMapping("/api/v1/user/usage-quotas")
	public UsageQuotaResponse get(@LoginUserId Long userId) {
		return UsageQuotaResponse.from(getUsageQuotasUseCase.getUsageQuotas(userId));
	}
}
