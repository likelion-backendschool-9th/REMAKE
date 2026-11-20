package com.remake.global.error;

import lombok.Getter;

/**
 * Service가 던지는 예외. 응답은 GlobalExceptionHandler가 만든다.
 * 예: throw new BusinessException(ErrorCode.INGREDIENT_NOT_FOUND);
 */
@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	// 사용자에게 보여줄 메시지를 상황에 맞게 바꿀 때 쓴다. 예: "남은 수량은 300g입니다."
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
