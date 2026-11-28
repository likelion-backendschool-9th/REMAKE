package com.remake.auth.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.auth.application.port.in.LogoutUseCase;
import com.remake.auth.application.port.in.RefreshAccessTokenUseCase;
import com.remake.auth.application.port.in.SocialLoginUseCase;
import com.remake.global.config.AuthProperties;
import com.remake.global.config.OpenApiConfig;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.security.AuthCookieFilter;
import com.remake.global.web.LoginUserId;
import com.remake.user.domain.AuthProvider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "인증")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

	private final SocialLoginUseCase socialLoginUseCase;
	private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
	private final LogoutUseCase logoutUseCase;
	private final AuthCookieWriter authCookieWriter;
	private final AuthProperties authProperties;

	@Operation(summary = "소셜 로그인", description = """
			카카오·네이버·구글 인가 코드로 로그인합니다. 처음이면 가입도 함께 처리합니다.
			성공하면 액세스·리프레시 토큰을 쿠키로 내려주고, 본문에는 온보딩 완료 여부만 담습니다.
			지원하지 않는 provider면 OAUTH_PROVIDER_UNSUPPORTED입니다.""")
	@PostMapping("/public/auth/oauth/{provider}")
	public OAuthLoginResponse oauthLogin(@PathVariable String provider,
			@Valid @RequestBody OAuthLoginRequest request,
			HttpServletResponse response) {
		AuthProvider authProvider = AuthProvider.fromPath(provider);
		if (authProvider == null) {
			throw new BusinessException(ErrorCode.OAUTH_PROVIDER_UNSUPPORTED);
		}
		SocialLoginUseCase.Result result = socialLoginUseCase.login(
				new SocialLoginUseCase.Command(authProvider, request.code(), request.redirectUri(), request.state()));
		authCookieWriter.writeLoginCookies(response, result.accessToken(), result.refreshToken(),
				result.refreshExpiresAt());
		return new OAuthLoginResponse(result.onboardingCompleted());
	}

	@Operation(summary = "액세스 토큰 재발급", description = """
			리프레시 토큰 쿠키로 새 액세스 토큰 쿠키를 발급합니다. 본문 없이 204를 돌려줍니다.""")
	@PostMapping("/public/auth/refresh")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = AuthCookieFilter.readCookie(request, authProperties.refreshCookieName());
		RefreshAccessTokenUseCase.Result result = refreshAccessTokenUseCase.refresh(refreshToken);
		authCookieWriter.writeAccessCookie(response, result.accessToken());
	}

	@Operation(summary = "로그아웃", description = """
			리프레시 토큰을 폐기하고 인증 쿠키를 지웁니다. 204를 돌려줍니다.""")
	@PostMapping("/user/auth/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
	public ResponseEntity<Void> logout(@LoginUserId Long userId, HttpServletRequest request,
			HttpServletResponse response) {
		String refreshToken = AuthCookieFilter.readCookie(request, authProperties.refreshCookieName());
		logoutUseCase.logout(userId, refreshToken);
		authCookieWriter.clearAuthCookies(response);
		return ResponseEntity.noContent().build();
	}
}
