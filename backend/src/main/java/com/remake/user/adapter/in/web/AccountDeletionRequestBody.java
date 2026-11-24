package com.remake.user.adapter.in.web;

import com.remake.user.application.port.in.DeleteAccountUseCase;
import com.remake.user.domain.DeletionMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * API 10 요청.
 * 카카오·구글: method=EMAIL_CODE, code.
 * 네이버: method=REAUTH, oauthCode, redirectUri, state(네이버 토큰 교환에 필요).
 */
public record AccountDeletionRequestBody(
		@NotNull DeletionMethod method,
		@Schema(description = "이메일 인증번호 6자리(EMAIL_CODE)") String code,
		@Schema(description = "네이버 재인증 인가 코드(REAUTH)") String oauthCode,
		@Schema(description = "네이버 재인증 redirectUri(REAUTH)") String redirectUri,
		@Schema(description = "네이버 OAuth state(REAUTH). 로그인과 같이 토큰 교환에 필요") String state) {

	DeleteAccountUseCase.Command toCommand(Long userId) {
		return new DeleteAccountUseCase.Command(userId, method, code, oauthCode, redirectUri, state);
	}
}
