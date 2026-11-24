package com.remake.global.error;

import lombok.Getter;

/** 요청 값을 함께 확인한 뒤 특정 입력 필드의 수정이 필요할 때 사용한다. */
@Getter
public class InvalidFieldException extends BusinessException {

	private final String field;

	public InvalidFieldException(String field, String message) {
		super(ErrorCode.INVALID_INPUT, message);
		this.field = field;
	}
}
