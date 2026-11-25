package com.remake.user.application.port.in;

/** 생성 후 하루가 지난 계정 삭제 인증번호 요청을 지운다. */
public interface PurgeExpiredDeletionCodesUseCase {

	int purgeExpired();
}
