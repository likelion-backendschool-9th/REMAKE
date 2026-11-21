package com.remake.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청.
 * state는 선택 필드이지만 네이버 토큰 교환에는 필수(없으면 OAUTH_FAILED).
 */
public record OAuthLoginRequest(
		@NotBlank String code,
		@NotBlank String redirectUri,
		String state) {
}
