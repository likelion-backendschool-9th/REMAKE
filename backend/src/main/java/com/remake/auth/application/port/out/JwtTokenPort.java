package com.remake.auth.application.port.out;

import java.time.Instant;

import com.remake.user.domain.UserRole;

/** JWT 발급·검증. 구현은 global/security에 둔다. */
public interface JwtTokenPort {

	record AccessClaims(Long userId, UserRole role, Instant expiresAt) {
	}

	record IssuedTokens(String accessToken, String refreshToken, Instant refreshExpiresAt) {
	}

	IssuedTokens issue(Long userId, UserRole role);

	/** 리프레시 때 액세스만 다시 발급한다. */
	String issueAccessToken(Long userId, UserRole role);

	AccessClaims parseAccessToken(String token);

	Long parseRefreshUserId(String token);
}
