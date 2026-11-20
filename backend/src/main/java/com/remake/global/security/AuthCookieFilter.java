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
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.user.domain.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 사용자를 SecurityContext에 넣는다.
 * 1) 테스트·내부용 요청 속성(LoginUserIdArgumentResolver.ATTRIBUTE)
 * 2) 액세스 토큰 HttpOnly 쿠키
 * 클라이언트가 요청 속성을 세팅할 수는 없다.
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
			Object attr = request.getAttribute(LoginUserIdArgumentResolver.ATTRIBUTE);
			if (attr instanceof Long userId) {
				setAuthentication(userId, UserRole.USER);
			} else {
				readAccessCookie(request);
			}
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
			setAuthentication(claims.userId(), claims.role());
		} catch (BusinessException ex) {
			if (ex.getErrorCode() == ErrorCode.TOKEN_EXPIRED) {
				request.setAttribute(TOKEN_EXPIRED_ATTR, Boolean.TRUE);
			}
		}
	}

	private void setAuthentication(Long userId, UserRole role) {
		LoginUserPrincipal principal = new LoginUserPrincipal(userId, role);
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);
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
