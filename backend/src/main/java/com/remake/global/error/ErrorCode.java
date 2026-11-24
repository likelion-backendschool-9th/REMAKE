package com.remake.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 모든 API 에러 코드. 응답의 code는 enum 이름 그대로 나간다.
 * 이름은 API 정의서·에러 정의서의 코드와 같게 쓴다. 기능별 코드는 기능을 만드는 PR에서 아래 "기능별" 영역에 추가한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "이용이 제한된 계정입니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "요청한 응답 형식을 지원하지 않습니다."),
	FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기가 너무 큽니다."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 생겼습니다. 잠시 후 다시 시도해 주세요."),
	SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "지금은 요청을 처리할 수 없습니다. 잠시 후 다시 시도해 주세요."),

	// 기능별 (기능을 만드는 PR에서 추가. 마지막 줄만 ;로 끝낸다)
	// 인증
	OAUTH_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),
	OAUTH_PROVIDER_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요."),
	// 재료
	INGREDIENT_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "재료 카테고리를 찾을 수 없습니다."),
	INGREDIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "재료를 찾을 수 없습니다."),
	INSUFFICIENT_QUANTITY(HttpStatus.BAD_REQUEST, "가진 양보다 많이 뺄 수 없습니다."),
	// AI 레시피 (문구는 에러 정의서 v2. 모두 횟수 차감 없음)
	AI_UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "레시피를 만들지 못했어요. 횟수는 차감되지 않았어요"),
	AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "생성이 오래 걸리고 있어요. 횟수는 차감되지 않았어요"),
	AI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "레시피를 만들지 못했어요. 횟수는 차감되지 않았어요"),
	// 이벤트
	EVENT_BATCH_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "이벤트는 한 번에 50개까지 보낼 수 있습니다.");

	private final HttpStatus status;
	private final String message;
}
