package com.remake.auth.application.port.in;

public interface RefreshAccessTokenUseCase {

	record Result(String accessToken) {
	}

	Result refresh(String refreshToken);
}
