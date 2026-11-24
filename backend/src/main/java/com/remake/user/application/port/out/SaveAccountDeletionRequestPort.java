package com.remake.user.application.port.out;

import java.time.LocalDateTime;

import com.remake.user.domain.AccountDeletionRequest;

public interface SaveAccountDeletionRequestPort {

	AccountDeletionRequest save(AccountDeletionRequest request);

	void deleteAllByUserId(Long userId);

	/** created_at이 before 이전인 행을 지우고 건수를 돌려준다 */
	int deleteCreatedBefore(LocalDateTime before);
}
