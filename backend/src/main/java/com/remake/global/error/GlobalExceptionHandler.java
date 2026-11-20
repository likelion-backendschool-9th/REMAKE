package com.remake.global.error;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 Controller의 예외를 ErrorResponse로 바꾸는 곳. Controller와 Service에서 try-catch로 에러 응답을 만들지 않는다.
 * 401/403은 Security 필터에서 막혀 여기를 거치지 않는다(global/security에서 같은 모양으로 응답).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		return respond(ErrorResponse.of(e.getErrorCode(), e.getMessage()));
	}

	// @Valid가 붙은 요청 본문 검증 실패: 어떤 필드가 왜 틀렸는지 fieldErrors로 알려 준다.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
		List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return respond(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
	}

	// 경로 변수·쿼리 파라미터에 붙은 검증(@Min 등) 실패
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e) {
		List<ErrorResponse.FieldError> fieldErrors = e.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return respond(ErrorResponse.of(ErrorCode.INVALID_INPUT, fieldErrors));
	}

	// JSON 형식 오류, 타입이 맞지 않는 값, 빠진 필수 파라미터
	@ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
			MissingServletRequestParameterException.class})
	public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
		return respond(ErrorResponse.of(ErrorCode.INVALID_INPUT));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
		return respond(ErrorResponse.of(ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		return respond(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException e) {
		return respond(ErrorResponse.of(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
	}

	// 업로드 파일이 spring.servlet.multipart의 크기 제한을 넘음
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ErrorResponse> handleUploadSize(MaxUploadSizeExceededException e) {
		return respond(ErrorResponse.of(ErrorCode.FILE_TOO_LARGE));
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

	private ResponseEntity<ErrorResponse> respond(ErrorResponse body) {
		return ResponseEntity.status(body.status()).body(body);
	}
}
