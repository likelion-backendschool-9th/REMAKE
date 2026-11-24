package com.remake.user.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.LoadAccountDeletionRequestPort;
import com.remake.user.application.port.out.SaveAccountDeletionRequestPort;
import com.remake.user.domain.AccountDeletionRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class AccountDeletionRequestPersistenceAdapter
		implements LoadAccountDeletionRequestPort, SaveAccountDeletionRequestPort {

	private final AccountDeletionRequestJpaRepository repository;

	@Override
	public Optional<AccountDeletionRequest> findLatestOpenByUserId(Long userId) {
		return repository.findFirstByUserIdAndVerifiedAtIsNullOrderByCreatedAtDescIdDesc(userId)
				.map(AccountDeletionRequestJpaEntity::toDomain);
	}

	@Override
	public AccountDeletionRequest save(AccountDeletionRequest request) {
		AccountDeletionRequestJpaEntity entity = request.id() == null
				? AccountDeletionRequestJpaEntity.from(request)
				: repository.findById(request.id()).map(existing -> {
					existing.apply(request);
					return existing;
				}).orElseGet(() -> AccountDeletionRequestJpaEntity.from(request));
		return repository.save(entity).toDomain();
	}

	@Override
	public void deleteAllByUserId(Long userId) {
		repository.deleteByUserId(userId);
	}

	@Override
	public int deleteCreatedBefore(LocalDateTime before) {
		return repository.deleteByCreatedAtBefore(before);
	}
}
