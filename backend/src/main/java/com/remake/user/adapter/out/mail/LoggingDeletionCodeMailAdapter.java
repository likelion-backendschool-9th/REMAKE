package com.remake.user.adapter.out.mail;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.SendDeletionCodeMailPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMTP 어댑터가 붙기 전까지 쓰는 기본 구현. 빈은 운영에도 남겨 앱이 뜨게 한다.
 * 이메일은 로그에 남기지 않고, 인증번호는 local·test 프로필에서만 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingDeletionCodeMailAdapter implements SendDeletionCodeMailPort {

	private final Environment environment;

	@Override
	public void send(String toEmail, String plainCode) {
		if (environment.matchesProfiles("local", "test")) {
			log.info("계정 삭제 인증번호 발송(mock): code={}", plainCode);
			return;
		}
		log.warn("계정 삭제 인증번호 메일 어댑터가 mock입니다. SMTP 어댑터를 연결하세요.");
	}
}
