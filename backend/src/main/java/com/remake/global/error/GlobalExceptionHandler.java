package com.remake.global.error;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 Controller의 예외를 ErrorResponse로 바꾸는 곳. Controller와 Service에서 try-catch로 에러 응답을 만들지 않는다.
 * 401/403은 Security 필터에서 막혀 여기를 거치지 않는다(global/security에서 같은 모양으로 응답).
 *
 * Spring MVC 기본 예외(JSON 형식 오류, 파라미터·헤더·쿠키·파일 누락, 타입 오류, 404, 405, 415, 413,
 * ResponseStatusException 등)는 ResponseEntityExceptionHandler가 원래 상태 코드로 처리한다.
 * 여기서는 handleExceptionInternal만 바꿔 본문을 ErrorResponse 모양으로 만들고, 헤더(405의 Allow 등)는 그대로 넘긴다.
 * 부모 클래스가 이미 처리하는 예외에 @ExceptionHandler를 또 만들면 앱이 뜰 때 오류가 나므로 만들지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		return respond(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
	}

	// JPA @Version이 다른 요청의 수정을 감지함
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
		return respond(ErrorResponse.of(ErrorCode.CONCURRENT_UPDATE));
	}

	// 예상하지 못한 오류: 서버 로그에는 전체 내용을 남기고, 사용자에게는 내부 내용을 숨긴다.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("처리하지 못한 예외", e);
		return respond(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
	}

	// Spring MVC 기본 예외의 응답 본문을 ErrorResponse로 바꾼다. 상태 코드에 맞는 공통 ErrorCode를 쓴다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ErrorCode errorCode = toErrorCode(statusCode);
		if (errorCode.getStatus().is5xxServerError()) {
			log.error("처리하지 못한 예외", ex);
		}
		ErrorResponse errorBody = switch (ex) {
			// @Valid가 붙은 요청 본문 검증 실패: 어떤 필드가 왜 틀렸는지 fieldErrors로 알려 준다.
			case MethodArgumentNotValidException e -> ErrorResponse.of(errorCode, bodyFieldErrors(e));
			// 경로 변수·쿼리 파라미터에 붙은 검증(@Min 등) 실패
			case HandlerMethodValidationException e -> ErrorResponse.of(errorCode, parameterFieldErrors(e));
			default -> ErrorResponse.of(errorCode);
		};
		return ResponseEntity.status(errorCode.getStatus()).headers(headers).body(errorBody);
	}

	// 상태 코드가 같은 공통 코드를 찾는다. 없으면 5xx는 INTERNAL_ERROR, 4xx는 INVALID_INPUT으로 보낸다.
	private ErrorCode toErrorCode(HttpStatusCode statusCode) {
		return Arrays.stream(ErrorCode.values())
				.filter(code -> code.getStatus().value() == statusCode.value())
				.findFirst()
				.orElse(statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.INVALID_INPUT);
	}

	private List<ErrorResponse.FieldError> bodyFieldErrors(MethodArgumentNotValidException e) {
		return e.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
	}

	private List<ErrorResponse.FieldError> parameterFieldErrors(HandlerMethodValidationException e) {
		return e.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
	}

	private ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
		return ResponseEntity.status(body.status()).body(body);
	}
}
