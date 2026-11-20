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

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class AuthController {

	private final SocialLoginUseCase socialLoginUseCase;
	private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
	private final LogoutUseCase logoutUseCase;
	private final AuthCookieWriter authCookieWriter;
	private final AuthProperties authProperties;

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

	@PostMapping("/public/auth/refresh")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void refresh(HttpServletRequest request, HttpServletResponse response) {
		String refreshToken = AuthCookieFilter.readCookie(request, authProperties.refreshCookieName());
		RefreshAccessTokenUseCase.Result result = refreshAccessTokenUseCase.refresh(refreshToken);
		authCookieWriter.writeAccessCookie(response, result.accessToken());
	}

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
