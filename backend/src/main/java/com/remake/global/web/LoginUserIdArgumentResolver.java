package com.remake.global.web;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.security.LoginUserPrincipal;

/**
 * @LoginUserId 인자에 로그인한 사용자 id를 넣는다.
 * AuthCookieFilter가 SecurityContext에 넣은 LoginUserPrincipal에서 읽는다.
 * 테스트는 요청 속성(ATTRIBUTE)으로 로그인 사용자를 넣을 수 있다(필터가 SecurityContext로 옮긴다).
 */
@Component
public class LoginUserIdArgumentResolver implements HandlerMethodArgumentResolver {

	public static final String ATTRIBUTE = "loginUserId";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(LoginUserId.class) && Long.class.equals(parameter.getParameterType());
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof LoginUserPrincipal principal) {
			return principal.userId();
		}
		throw new BusinessException(ErrorCode.UNAUTHORIZED);
	}
}
