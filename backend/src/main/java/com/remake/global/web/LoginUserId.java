package com.remake.global.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * 컨트롤러 인자에 붙이면 로그인한 사용자의 id(Long)를 넣어 준다. 사용자 id를 요청 본문·경로·쿼리로 받지 않는다.
 * 값을 꺼내는 방법은 {@link LoginUserIdArgumentResolver} 한 곳에만 있다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface LoginUserId {
}
