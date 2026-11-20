package com.remake.user.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.remake.user.application.port.out.LoadUserPort;
import com.remake.user.application.port.out.SaveUserPort;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class UserPersistenceAdapter implements LoadUserPort, SaveUserPort {

	private final UserJpaRepository repository;

	@Override
	public Optional<User> findById(Long id) {
		return repository.findById(id).map(UserJpaEntity::toDomain);
	}

	@Override
	public Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId) {
		return repository.findByProviderAndProviderUserId(provider, providerUserId).map(UserJpaEntity::toDomain);
	}

	@Override
	public User save(User user) {
		UserJpaEntity entity = user.id() == null
				? UserJpaEntity.from(user)
				: repository.findById(user.id()).map(existing -> {
					existing.apply(user);
					return existing;
				}).orElseGet(() -> UserJpaEntity.from(user));
		return repository.save(entity).toDomain();
	}
}
