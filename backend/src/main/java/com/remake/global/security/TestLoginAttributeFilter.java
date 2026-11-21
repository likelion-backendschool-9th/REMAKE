package com.remake.global.security;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.user.domain.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 통합 테스트 전용. 요청 속성({@link LoginUserIdArgumentResolver#ATTRIBUTE})으로 로그인을 흉내 낸다.
 * 운영·로컬 프로필에는 등록되지 않는다.
 */
@Component
@Profile("test")
public class TestLoginAttributeFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			Object attr = request.getAttribute(LoginUserIdArgumentResolver.ATTRIBUTE);
			if (attr instanceof Long userId) {
				LoginUserPrincipal principal = new LoginUserPrincipal(userId, UserRole.USER);
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		filterChain.doFilter(request, response);
	}
}
