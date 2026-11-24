package com.remake.auth.application.port.out;

import com.remake.auth.domain.RefreshToken;

public interface SaveRefreshTokenPort {

	RefreshToken save(RefreshToken token);

	void deleteAllByUserId(Long userId);
}
