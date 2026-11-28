package com.remake.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.global.config.AuthProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 액세스 토큰 HttpOnly 쿠키를 읽어 SecurityContext에 로그인 사용자를 넣는다.
 * 통합 테스트의 요청 속성 로그인은 {@link OptionalAuthPreFilter}로 이 필터 앞에 끼운다.
 */
@Component
@RequiredArgsConstructor
public class AuthCookieFilter extends OncePerRequestFilter {

	static final String TOKEN_EXPIRED_ATTR = "authTokenExpired";

	private final AuthProperties authProperties;
	private final JwtTokenPort jwtTokenPort;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			readAccessCookie(request);
		}
		filterChain.doFilter(request, response);
	}

	private void readAccessCookie(HttpServletRequest request) {
		String token = readCookie(request, authProperties.accessCookieName());
		if (token == null || token.isBlank()) {
			return;
		}
		try {
			JwtTokenPort.AccessClaims claims = jwtTokenPort.parseAccessToken(token);
			LoginUserPrincipal principal = new LoginUserPrincipal(claims.userId(), claims.role());
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (BusinessException ex) {
			if (ex.getErrorCode() == ErrorCode.TOKEN_EXPIRED) {
				request.setAttribute(TOKEN_EXPIRED_ATTR, Boolean.TRUE);
			}
		}
	}

	public static String readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
