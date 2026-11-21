package com.remake.auth.application.port.out;

import java.util.Optional;

import com.remake.auth.domain.RefreshToken;

public interface LoadRefreshTokenPort {

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
