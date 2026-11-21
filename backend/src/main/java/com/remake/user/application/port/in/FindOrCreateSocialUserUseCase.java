package com.remake.user.application.port.in;

import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;

/** 소셜 프로필로 회원을 찾거나 새로 만든다. 인증 기능이 로그인 때 호출한다. */
public interface FindOrCreateSocialUserUseCase {

	record Result(User user, boolean newUser) {
	}

	Result findOrCreate(AuthProvider provider, String providerUserId, String email);
}
