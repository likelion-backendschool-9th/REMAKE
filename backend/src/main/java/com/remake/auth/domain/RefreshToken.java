package com.remake.auth.domain;

import java.time.LocalDateTime;

public record RefreshToken(
		Long id,
		Long userId,
		String tokenHash,
		LocalDateTime expiresAt,
		LocalDateTime revokedAt) {

	public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime expiresAt) {
		return new RefreshToken(null, userId, tokenHash, expiresAt, null);
	}

	public boolean isValid(LocalDateTime now) {
		return revokedAt == null && expiresAt.isAfter(now);
	}

	public RefreshToken revoke(LocalDateTime revokedAt) {
		return new RefreshToken(id, userId, tokenHash, expiresAt, revokedAt);
	}
}
