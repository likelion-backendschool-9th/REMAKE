package com.remake.user.domain;

/** 소셜 로그인 제공자. DB·이벤트·API path와 같은 이름을 쓴다. */
public enum AuthProvider {
	KAKAO,
	NAVER,
	GOOGLE;

	public String wireName() {
		return name().toLowerCase();
	}

	public static AuthProvider fromPath(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return AuthProvider.valueOf(value.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
