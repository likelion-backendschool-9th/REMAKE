package com.remake.user.application.port.out;

import java.time.LocalDateTime;
import java.util.OptionalInt;

import com.remake.user.domain.AccountDeletionRequest;

public interface SaveAccountDeletionRequestPort {

	AccountDeletionRequest save(AccountDeletionRequest request);

	/**
	 * 틀린 시도를 DB에서 원자적으로 +1 한다. verified이거나 이미 잠긴 행이면 empty.
	 * 바깥 트랜잭션과 분리된 REQUIRES_NEW 안에서 호출한다.
	 */
	OptionalInt incrementWrongAttempts(Long id);

	void deleteAllByUserId(Long userId);

	/** created_at이 before 이전인 행을 지우고 건수를 돌려준다 */
	int deleteCreatedBefore(LocalDateTime before);
}
