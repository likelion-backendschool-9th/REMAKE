package com.remake.user.application.port.in;

import java.time.LocalDateTime;

/** API 9: 카카오·구글 회원에게 계정 삭제 이메일 인증번호를 보낸다(재전송 포함). */
public interface RequestAccountDeletionCodeUseCase {

	LocalDateTime requestCode(Long userId);
}
