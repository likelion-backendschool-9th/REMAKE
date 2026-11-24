package com.remake.user.adapter.out.mail;

import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.SendDeletionCodeMailPort;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬·기본 구현. 실제 SMTP는 배포 환경 변수가 준비되면 별도 어댑터로 교체한다.
 * 운영에서도 키/SMTP가 비어 있으면 이 구현이 쓰이므로 번호가 로그에 남을 수 있다.
 */
@Slf4j
@Component
public class LoggingDeletionCodeMailAdapter implements SendDeletionCodeMailPort {

	@Override
	public void send(String toEmail, String plainCode) {
		log.info("계정 삭제 인증번호 발송(mock): to={}, code={}", toEmail, plainCode);
	}
}
