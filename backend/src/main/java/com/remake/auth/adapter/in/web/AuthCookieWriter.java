package com.remake.auth.adapter.in.web;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.remake.global.config.AuthProperties;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthCookieWriter {

	private final AuthProperties authProperties;
	private final Clock clock;

	public void writeLoginCookies(HttpServletResponse response, String accessToken, String refreshToken,
			Instant refreshExpiresAt) {
		long refreshMaxAge = Math.max(0, Duration.between(clock.instant(), refreshExpiresAt).getSeconds());
		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie(accessToken, authProperties.accessTokenTtlMinutes() * 60)
				.toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken, refreshMaxAge).toString());
	}

	public void writeAccessCookie(HttpServletResponse response, String accessToken) {
		response.addHeader(HttpHeaders.SET_COOKIE,
				accessCookie(accessToken, authProperties.accessTokenTtlMinutes() * 60).toString());
	}

	public void clearAuthCookies(HttpServletResponse response) {
		response.addHeader(HttpHeaders.SET_COOKIE, accessCookie("", 0).toString());
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString());
	}

	private ResponseCookie accessCookie(String value, long maxAgeSeconds) {
		return baseCookie(authProperties.accessCookieName(), value, maxAgeSeconds);
	}

	private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
		return baseCookie(authProperties.refreshCookieName(), value, maxAgeSeconds);
	}

	private ResponseCookie baseCookie(String name, String value, long maxAgeSeconds) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(authProperties.cookieSecure())
				.sameSite("Lax")
				.path("/")
				.maxAge(maxAgeSeconds)
				.build();
	}
}
