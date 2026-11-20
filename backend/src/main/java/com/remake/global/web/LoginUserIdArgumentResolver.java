package com.remake.global.web;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;

/**
 * @LoginUserId 인자에 로그인한 사용자 id를 넣는다.
 * 임시 구현: 로그인(#66)이 아직 없어서 요청 속성(ATTRIBUTE)에서 읽고, 없으면 UNAUTHORIZED(401)를 던진다.
 * 요청 속성은 서버 코드만 채울 수 있어 클라이언트가 값을 보낼 수 없다.
 * TODO(#66): 이 클래스의 resolveArgument만 SecurityContext에서 사용자 id를 꺼내도록 바꾼다. 컨트롤러와 테스트는 바뀌지 않는다.
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
		Object userId = webRequest.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
		if (userId instanceof Long id) {
			return id;
		}
		throw new BusinessException(ErrorCode.UNAUTHORIZED);
	}
}
