package com.remake.auth.application.port.in;

import com.remake.user.domain.AuthProvider;

public interface SocialLoginUseCase {

	record Command(AuthProvider provider, String code, String redirectUri, String state) {
	}

	record Result(boolean onboardingCompleted, String accessToken, String refreshToken,
			java.time.Instant refreshExpiresAt) {
	}

	Result login(Command command);
}
