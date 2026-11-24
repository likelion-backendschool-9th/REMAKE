package com.remake.user.application.port.out;

/**
 * 계정 삭제 인증번호를 이메일로 보낸다. 로컬·테스트는 로그 mock을 쓴다.
 */
public interface SendDeletionCodeMailPort {

	void send(String toEmail, String plainCode);
}
