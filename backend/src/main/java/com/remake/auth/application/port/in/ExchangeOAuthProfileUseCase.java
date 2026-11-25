package com.remake.auth.application.port.in;

import com.remake.auth.domain.SocialProfile;
import com.remake.user.domain.AuthProvider;

/** 소셜 인가 코드를 프로필로 바꾼다. 로그인·네이버 탈퇴 재인증이 쓴다. */
public interface ExchangeOAuthProfileUseCase {

	SocialProfile exchange(AuthProvider provider, String code, String redirectUri, String state);
}
