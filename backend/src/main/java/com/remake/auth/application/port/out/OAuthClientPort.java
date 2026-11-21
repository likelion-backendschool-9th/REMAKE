package com.remake.auth.application.port.out;

import com.remake.auth.domain.SocialProfile;
import com.remake.user.domain.AuthProvider;

/** 소셜 인가 코드를 프로필로 바꾼다. 어댑터가 구현하고 통합 테스트에서는 어댑터 클래스를 가짜로 바꾼다. */
public interface OAuthClientPort {

	/**
	 * @param state 네이버 토큰 교환에 필요. 없으면 null.
	 */
	SocialProfile exchange(AuthProvider provider, String code, String redirectUri, String state);
}
