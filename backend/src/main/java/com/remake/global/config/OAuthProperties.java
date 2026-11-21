package com.remake.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remake.oauth")
public record OAuthProperties(
		boolean mockEnabled,
		Provider kakao,
		Provider naver,
		Provider google) {

	public record Provider(String clientId, String clientSecret, String tokenUri, String userInfoUri) {
	}
}
