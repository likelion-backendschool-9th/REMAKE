package com.remake.user.application.port.out;

import java.util.Optional;

import com.remake.user.domain.AccountDeletionRequest;

public interface LoadAccountDeletionRequestPort {

	/** 회원에게 아직 확인되지 않은 요청 중 가장 최근 것 */
	Optional<AccountDeletionRequest> findLatestOpenByUserId(Long userId);
}
