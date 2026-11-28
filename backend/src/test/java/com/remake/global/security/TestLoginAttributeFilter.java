package com.remake.global.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.user.domain.UserRole;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 통합 테스트 전용. 요청 속성({@link LoginUserIdArgumentResolver#ATTRIBUTE})으로 로그인을 흉내 낸다.
 * {@code IntegrationTestSupport}가 빈으로 등록한다. {@code src/main}에는 두지 않는다.
 */
public class TestLoginAttributeFilter extends OncePerRequestFilter implements OptionalAuthPreFilter {

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
