package com.remake.user.application.port.in;

import com.remake.user.domain.DeletionMethod;

/** API 10: 이메일 인증번호 또는 네이버 재인증으로 계정을 삭제한다. */
public interface DeleteAccountUseCase {

	record Command(
			Long userId,
			DeletionMethod method,
			String code,
			String oauthCode,
			String redirectUri,
			String state) {
	}

	void delete(Command command);
}
