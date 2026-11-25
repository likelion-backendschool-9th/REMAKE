package com.remake.auth.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.auth.application.port.out.LoadRefreshTokenPort;
import com.remake.auth.application.port.out.SaveRefreshTokenPort;
import com.remake.auth.domain.RefreshToken;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class RefreshTokenPersistenceAdapter implements LoadRefreshTokenPort, SaveRefreshTokenPort {

	private final RefreshTokenJpaRepository repository;

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return repository.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
	}

	@Override
	public RefreshToken save(RefreshToken token) {
		RefreshTokenJpaEntity entity = token.id() == null
				? RefreshTokenJpaEntity.from(token)
				: repository.findById(token.id()).map(existing -> {
					existing.apply(token);
					return existing;
				}).orElseGet(() -> RefreshTokenJpaEntity.from(token));
		return repository.save(entity).toDomain();
	}

	@Override
	public void deleteAllByUserId(Long userId) {
		repository.deleteByUserId(userId);
	}
}
