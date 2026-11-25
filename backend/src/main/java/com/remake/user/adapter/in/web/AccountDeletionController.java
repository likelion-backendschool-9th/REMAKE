package com.remake.user.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.auth.adapter.in.web.AuthCookieWriter;
import com.remake.global.config.OpenApiConfig;
import com.remake.global.web.LoginUserId;
import com.remake.user.application.port.in.DeleteAccountUseCase;
import com.remake.user.application.port.in.RequestAccountDeletionCodeUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "계정 삭제")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/account")
@RequiredArgsConstructor
public class AccountDeletionController {

	private final RequestAccountDeletionCodeUseCase requestAccountDeletionCodeUseCase;
	private final DeleteAccountUseCase deleteAccountUseCase;
	private final AuthCookieWriter authCookieWriter;

	@Operation(summary = "계정 삭제 인증번호 발송", description = """
			카카오·구글 회원의 이메일로 6자리 인증번호를 보냅니다(5분 유효). 재전송도 같은 API입니다.
			이메일이 없으면 ACCOUNT_EMAIL_MISSING입니다. 네이버는 재인증으로 삭제합니다.""")
	@PostMapping("/deletion-code")
	@ResponseStatus(HttpStatus.CREATED)
	public AccountDeletionCodeResponse requestCode(@LoginUserId Long userId) {
		return new AccountDeletionCodeResponse(requestAccountDeletionCodeUseCase.requestCode(userId));
	}

	@Operation(summary = "계정 삭제", description = """
			카카오·구글은 EMAIL_CODE, 네이버는 REAUTH로 본인 확인 후 탈퇴합니다.
			성공하면 쿠키를 지우고 204입니다.""")
	@PostMapping("/deletion")
	public ResponseEntity<Void> delete(@LoginUserId Long userId, @Valid @RequestBody AccountDeletionRequestBody body,
			HttpServletResponse response) {
		deleteAccountUseCase.delete(body.toCommand(userId));
		authCookieWriter.clearAuthCookies(response);
		return ResponseEntity.noContent().build();
	}
}
