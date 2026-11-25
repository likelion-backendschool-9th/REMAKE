package com.remake.auth.application.port.in;

/** 탈퇴 등으로 회원의 리프레시 토큰 행을 모두 지운다. */
public interface DeleteRefreshTokensForUserUseCase {

	void deleteAllForUser(Long userId);
}
