package com.remake.global.error;

import java.util.Map;

import lombok.Getter;

/**
 * Service가 던지는 예외. 응답은 GlobalExceptionHandler가 만든다.
 * 예: throw new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;
	private final Map<String, Object> details;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.getMessage(), null);
	}

	// 사용자에게 보여줄 메시지를 상황에 맞게 바꿀 때 쓴다. 예: "남은 수량은 300g입니다."
	public BusinessException(ErrorCode errorCode, String message) {
		this(errorCode, message, null);
	}

	// 화면이 쓰는 값을 응답의 details에 함께 담을 때 쓴다. 예: Map.of("remaining", 300)
	public BusinessException(ErrorCode errorCode, String message, Map<String, Object> details) {
		super(message);
		this.errorCode = errorCode;
		this.details = details;
	}
}
