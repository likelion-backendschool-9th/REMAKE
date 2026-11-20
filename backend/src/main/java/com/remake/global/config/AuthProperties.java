package com.remake.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "remake.auth")
public record AuthProperties(
		String jwtSecret,
		long accessTokenTtlMinutes,
		long refreshTokenTtlDays,
		boolean cookieSecure,
		String accessCookieName,
		String refreshCookieName) {
}
