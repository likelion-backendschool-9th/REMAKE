package com.remake.auth.adapter.in.web;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청. API 정의서의 code·redirectUri에 더해, 네이버 토큰 교환용 state를 선택적으로 받는다.
 */
public record OAuthLoginRequest(
		@NotBlank String code,
		@NotBlank String redirectUri,
		String state) {
}
