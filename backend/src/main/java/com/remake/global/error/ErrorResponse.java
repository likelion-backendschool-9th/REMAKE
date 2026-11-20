package com.remake.global.error;

import java.util.List;

import org.slf4j.MDC;

import com.remake.global.common.TraceIdFilter;

/**
 * 모든 API의 에러 응답. 모양: { code, message, status, traceId, fieldErrors }
 * fieldErrors는 입력값 검증 실패일 때만 값이 있고, 그 밖에는 null이다.
 * Security의 401/403 응답(AuthenticationEntryPoint, AccessDeniedHandler)도 이 모양을 쓴다.
 */
public record ErrorResponse(String code, String message, int status, String traceId, List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, errorCode.getStatus().value(), currentTraceId(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errorCode.getStatus().value(),
				currentTraceId(), fieldErrors);
	}

	private static String currentTraceId() {
		return MDC.get(TraceIdFilter.MDC_KEY);
	}
}
