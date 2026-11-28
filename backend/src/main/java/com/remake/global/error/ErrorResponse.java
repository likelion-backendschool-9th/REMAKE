package com.remake.global.error;

import java.util.List;
import java.util.Map;

import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.remake.global.common.TraceIdFilter;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 모든 API의 에러 응답. 모양: { code, message, status, traceId, fieldErrors, details? }
 * fieldErrors는 입력값 검증 실패일 때만 값이 있고, 그 밖에는 null이다.
 * details는 화면이 쓰는 값(예: 남은 양 remaining)이 있을 때만 나가고, 없으면 필드 자체가 빠진다.
 * Security의 401/403 응답(AuthenticationEntryPoint, AccessDeniedHandler)도 이 모양을 쓴다.
 */
public record ErrorResponse(String code, String message, int status,
		@Schema(nullable = true) String traceId,
		@Schema(nullable = true) List<FieldError> fieldErrors,
		@Schema(nullable = true) @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> details) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(ErrorCode errorCode) {
		return of(errorCode, errorCode.getMessage());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(errorCode.name(), message, errorCode.getStatus().value(), currentTraceId(), null, null);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message, Map<String, Object> details) {
		return new ErrorResponse(errorCode.name(), message, errorCode.getStatus().value(), currentTraceId(), null, details);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
		return new ErrorResponse(errorCode.name(), errorCode.getMessage(), errorCode.getStatus().value(),
				currentTraceId(), fieldErrors, null);
	}

	private static String currentTraceId() {
		return MDC.get(TraceIdFilter.MDC_KEY);
	}
}
