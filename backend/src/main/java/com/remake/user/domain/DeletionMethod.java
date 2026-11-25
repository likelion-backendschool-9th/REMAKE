package com.remake.user.domain;

/**
 * 계정 삭제 본인 확인 방식. 이벤트 deletion_method 값(email_code / reauth)과 맞춘다.
 */
public enum DeletionMethod {

	EMAIL_CODE("email_code"),
	REAUTH("reauth");

	private final String eventValue;

	DeletionMethod(String eventValue) {
		this.eventValue = eventValue;
	}

	/** events.md 허용 값 */
	public String eventValue() {
		return eventValue;
	}
}
